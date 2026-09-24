package com.rikkeibank.transaction.saga;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.enums.TransactionStatus;
import com.rikkeibank.common.enums.TransactionType;
import com.rikkeibank.common.events.TransferCompletedEvent;
import com.rikkeibank.common.events.TransferFailedEvent;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.transaction.client.AccountClient;
import com.rikkeibank.transaction.client.dto.AccountActionRequest;
import com.rikkeibank.transaction.dto.TransactionResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.kafka.TransactionEventProducer;
import com.rikkeibank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferSagaOrchestrator {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionEventProducer eventProducer;

    public TransactionResponse executeTransferSaga(TransferRequest request, Long tellerId, String username) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new AppException(ErrorCode.SAME_ACCOUNT_TRANSFER, "Cannot transfer money to the same account");
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("Starting Transfer Saga [txId={}]: from {} to {}, amount: {}", 
                transactionId, request.getFromAccountNumber(), request.getToAccountNumber(), request.getAmount());

        // Step 0: Create Initial Transaction Record in PENDING state
        TransactionRecord record = TransactionRecord.builder()
                .transactionId(transactionId)
                .fromAccountNumber(request.getFromAccountNumber())
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .currency("VND")
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .note(request.getNote())
                .tellerId(tellerId)
                .initiatedBy(username)
                .createdAt(Instant.now())
                .build();
        record = transactionRepository.save(record);

        // Step 1: Debit Source Account
        boolean debited = false;
        try {
            executeDebitWithCircuitBreaker(request.getFromAccountNumber(), AccountActionRequest.builder()
                    .amount(request.getAmount())
                    .transactionId(transactionId)
                    .reason("Transfer to " + request.getToAccountNumber())
                    .build());
            debited = true;
            log.info("Saga [txId={}] Step 1 (Debit) SUCCEEDED", transactionId);
        } catch (Exception e) {
            log.error("Saga [txId={}] Step 1 (Debit) FAILED: {}", transactionId, e.getMessage());
            record.setStatus(TransactionStatus.FAILED);
            record.setFailureReason("Debit failed: " + e.getMessage());
            record.setCompletedAt(Instant.now());
            transactionRepository.save(record);

            eventProducer.publishTransferFailed(TransferFailedEvent.builder()
                    .transactionId(transactionId)
                    .fromAccount(request.getFromAccountNumber())
                    .toAccount(request.getToAccountNumber())
                    .amount(request.getAmount())
                    .reason("Debit failed: " + e.getMessage())
                    .timestamp(Instant.now())
                    .build());

            throw (e instanceof AppException appEx) ? appEx : new AppException(ErrorCode.BAD_REQUEST, "Debit failed: " + e.getMessage());
        }

        // Step 2: Credit Destination Account
        try {
            executeCreditWithCircuitBreaker(request.getToAccountNumber(), AccountActionRequest.builder()
                    .amount(request.getAmount())
                    .transactionId(transactionId)
                    .reason("Received from " + request.getFromAccountNumber())
                    .build());
            log.info("Saga [txId={}] Step 2 (Credit) SUCCEEDED", transactionId);
        } catch (Exception e) {
            log.warn("Saga [txId={}] Step 2 (Credit) FAILED: {}. Initiating COMPENSATING action (rollback)...", 
                    transactionId, e.getMessage());
            record.setStatus(TransactionStatus.COMPENSATING);
            transactionRepository.save(record);

            // COMPENSATING TRANSACTION: Refund debited amount back to source account
            try {
                executeCompensateWithCircuitBreaker(request.getFromAccountNumber(), AccountActionRequest.builder()
                        .amount(request.getAmount())
                        .transactionId(transactionId)
                        .reason("Compensating refund for failed transfer")
                        .build());
                log.info("Saga [txId={}] COMPENSATING TRANSACTION SUCCEEDED (Funds refunded to source)", transactionId);
                record.setStatus(TransactionStatus.FAILED);
                record.setFailureReason("Credit to target failed (" + e.getMessage() + "). Funds successfully refunded to source.");
            } catch (Exception compEx) {
                log.error("CRITICAL: Saga [txId={}] COMPENSATING TRANSACTION FAILED: {}", transactionId, compEx.getMessage());
                record.setStatus(TransactionStatus.FAILED);
                record.setFailureReason("Credit failed AND compensating refund failed: " + compEx.getMessage());
            }

            record.setCompletedAt(Instant.now());
            TransactionRecord failedRecord = transactionRepository.save(record);

            // Publish failure event to Kafka
            eventProducer.publishTransferFailed(TransferFailedEvent.builder()
                    .transactionId(transactionId)
                    .fromAccount(request.getFromAccountNumber())
                    .toAccount(request.getToAccountNumber())
                    .amount(request.getAmount())
                    .reason(record.getFailureReason())
                    .timestamp(Instant.now())
                    .build());

            throw new AppException(ErrorCode.BAD_REQUEST, record.getFailureReason());
        }

        // All steps succeeded: Mark as SUCCESS
        record.setStatus(TransactionStatus.SUCCESS);
        record.setCompletedAt(Instant.now());
        TransactionRecord savedRecord = transactionRepository.save(record);

        // Publish success event to Kafka
        eventProducer.publishTransferCompleted(TransferCompletedEvent.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccountNumber())
                .toAccount(request.getToAccountNumber())
                .amount(request.getAmount())
                .note(request.getNote())
                .status("SUCCESS")
                .timestamp(Instant.now())
                .build());

        log.info("Saga [txId={}] COMPLETED SUCCESSFULLY!", transactionId);
        return mapToResponse(savedRecord);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "accountServiceFallback")
    protected ApiResponse<Map<String, Object>> executeDebitWithCircuitBreaker(String account, AccountActionRequest req) {
        return accountClient.debit(account, req);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "accountServiceFallback")
    protected ApiResponse<Map<String, Object>> executeCreditWithCircuitBreaker(String account, AccountActionRequest req) {
        return accountClient.credit(account, req);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "accountServiceFallback")
    protected ApiResponse<Map<String, Object>> executeCompensateWithCircuitBreaker(String account, AccountActionRequest req) {
        return accountClient.compensateDebit(account, req);
    }

    public ApiResponse<Map<String, Object>> accountServiceFallback(String account, AccountActionRequest req, Throwable t) {
        log.error("Resilience4j Circuit Breaker OPEN/HALF_OPEN or Fallback invoked: {}", t.getMessage());
        throw new AppException(ErrorCode.CIRCUIT_BREAKER_OPEN, "Account service is currently unavailable: " + t.getMessage());
    }

    public TransactionResponse mapToResponse(TransactionRecord r) {
        return TransactionResponse.builder()
                .id(r.getId())
                .transactionId(r.getTransactionId())
                .fromAccountNumber(r.getFromAccountNumber())
                .toAccountNumber(r.getToAccountNumber())
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .transactionType(r.getTransactionType())
                .status(r.getStatus())
                .note(r.getNote())
                .failureReason(r.getFailureReason())
                .tellerId(r.getTellerId())
                .initiatedBy(r.getInitiatedBy())
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }
}
