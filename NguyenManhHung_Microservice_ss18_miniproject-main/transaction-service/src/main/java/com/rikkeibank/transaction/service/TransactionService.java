package com.rikkeibank.transaction.service;

import com.rikkeibank.common.dto.PageResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.transaction.dto.TransactionResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.entity.TransactionRecord;
import com.rikkeibank.transaction.repository.TransactionRepository;
import com.rikkeibank.transaction.saga.TransferSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransferSagaOrchestrator sagaOrchestrator;

    public TransactionResponse transfer(TransferRequest request, Long userId, String username, String roles) {
        Long tellerId = null;
        if (roles != null && roles.contains("ROLE_TELLER")) {
            tellerId = userId;
        }
        return sagaOrchestrator.executeTransferSaga(request, tellerId, username);
    }

    public TransactionResponse getTransaction(String transactionId) {
        TransactionRecord record = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        return sagaOrchestrator.mapToResponse(record);
    }

    public PageResponse<TransactionResponse> getTransactionsByAccount(String accountNumber, int page, int size) {
        Page<TransactionRecord> p = transactionRepository
                .findByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(accountNumber, accountNumber, PageRequest.of(page, size));
        return toPageResponse(p);
    }

    public PageResponse<TransactionResponse> getDailyTransactionsForTeller(Long tellerId, int page, int size) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = Instant.now();

        log.info("Fetching daily transactions for tellerId: {} from {} to {}", tellerId, startOfDay, endOfDay);
        Page<TransactionRecord> p = transactionRepository
                .findByTellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(tellerId, startOfDay, endOfDay, PageRequest.of(page, size));
        return toPageResponse(p);
    }

    public PageResponse<TransactionResponse> getDailyTransactionsAdmin(int page, int size) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = Instant.now();

        Page<TransactionRecord> p = transactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startOfDay, endOfDay, PageRequest.of(page, size));
        return toPageResponse(p);
    }

    private PageResponse<TransactionResponse> toPageResponse(Page<TransactionRecord> p) {
        return PageResponse.<TransactionResponse>builder()
                .content(p.getContent().stream().map(sagaOrchestrator::mapToResponse).toList())
                .pageNumber(p.getNumber())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .last(p.isLast())
                .build();
    }
}
