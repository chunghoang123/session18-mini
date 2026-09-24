package com.rikkeibank.account.service;

import com.rikkeibank.account.client.CustomerClient;
import com.rikkeibank.account.dto.*;
import com.rikkeibank.account.entity.BankAccount;
import com.rikkeibank.account.repository.BankAccountRepository;
import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.enums.AccountStatus;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.exception.InsufficientBalanceException;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final CustomerClient customerClient;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public AccountResponse openAccount(OpenAccountRequest request) {
        log.info("Opening account for customerId: {}, type: {}", request.getCustomerId(), request.getAccountType());

        // Validate customer exists via OpenFeign call to customer-service
        try {
            ApiResponse<Map<String, Object>> customerResp = customerClient.getCustomerById(request.getCustomerId());
            if (customerResp == null || customerResp.getCode() != 200 || customerResp.getData() == null) {
                log.warn("Customer validation failed for customerId: {}", request.getCustomerId());
                throw new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with id: " + request.getCustomerId());
            }
        } catch (Exception e) {
            log.warn("Could not verify customer via Feign: {}", e.getMessage());
            // In case customer service is down or standalone test, proceed if it's not explicitly a 404
            if (e instanceof ResourceNotFoundException) {
                throw e;
            }
        }

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal minBalance = "SAVINGS".equalsIgnoreCase(request.getAccountType()) 
                ? BigDecimal.valueOf(1000000) 
                : BigDecimal.valueOf(50000);

        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumber)
                .customerId(request.getCustomerId())
                .accountType(request.getAccountType().toUpperCase())
                .balance(request.getInitialDeposit())
                .minimumBalance(minBalance)
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        BankAccount saved = accountRepository.save(account);
        log.info("Opened account: {} with balance: {}", saved.getAccountNumber(), saved.getBalance());
        return mapToResponse(saved);
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountResponse getAccount(String accountNumber) {
        log.info("Fetching account from DB for accountNumber: {}", accountNumber);
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountNumber));
        return mapToResponse(account);
    }

    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public AccountResponse updateStatus(String accountNumber, AccountStatus status) {
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found: " + accountNumber));

        account.setStatus(status);
        account.setUpdatedAt(Instant.now());
        BankAccount updated = accountRepository.save(account);
        log.info("Updated account {} status to: {}", accountNumber, status);
        return mapToResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public AccountResponse debit(String accountNumber, DebitRequest request) {
        log.info("Saga Step 1: Debiting account: {}, amount: {}, txId: {}", accountNumber, request.getAmount(), request.getTransactionId());
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Source account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED, "Source account is not active. Status: " + account.getStatus());
        }

        BigDecimal availableBalance = account.getBalance().subtract(account.getMinimumBalance());
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient funds. Available: %s, Required: %s", availableBalance, request.getAmount()));
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        account.setUpdatedAt(Instant.now());
        BankAccount saved = accountRepository.save(account);
        log.info("Debited {} successfully. New balance: {}", accountNumber, saved.getBalance());
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public AccountResponse credit(String accountNumber, CreditRequest request) {
        log.info("Saga Step 2: Crediting account: {}, amount: {}, txId: {}", accountNumber, request.getAmount(), request.getTransactionId());
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Destination account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED, "Destination account is locked or inactive");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        account.setUpdatedAt(Instant.now());
        BankAccount saved = accountRepository.save(account);
        log.info("Credited {} successfully. New balance: {}", accountNumber, saved.getBalance());
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public AccountResponse compensateDebit(String accountNumber, DebitRequest request) {
        log.warn("Saga Compensating Action: Refunding debited amount: {} back to account: {}, txId: {}", 
                request.getAmount(), accountNumber, request.getTransactionId());
        BankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found for refund: " + accountNumber));

        account.setBalance(account.getBalance().add(request.getAmount()));
        account.setUpdatedAt(Instant.now());
        BankAccount saved = accountRepository.save(account);
        log.info("Compensated refund for {} completed. New balance: {}", accountNumber, saved.getBalance());
        return mapToResponse(saved);
    }

    private String generateUniqueAccountNumber() {
        String num;
        do {
            long rand = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            num = String.valueOf(rand);
        } while (accountRepository.existsByAccountNumber(num));
        return num;
    }

    private AccountResponse mapToResponse(BankAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .minimumBalance(account.getMinimumBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
