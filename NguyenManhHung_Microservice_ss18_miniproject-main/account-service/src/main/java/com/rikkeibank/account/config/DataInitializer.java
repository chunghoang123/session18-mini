package com.rikkeibank.account.config;

import com.rikkeibank.account.entity.BankAccount;
import com.rikkeibank.account.repository.BankAccountRepository;
import com.rikkeibank.common.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BankAccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            log.info("Seeding initial Bank Accounts...");

            // Active account with 50,000,000 VND balance
            accountRepository.save(BankAccount.builder()
                    .accountNumber("1000000001")
                    .customerId(1L)
                    .accountType("CHECKING")
                    .balance(BigDecimal.valueOf(50000000))
                    .minimumBalance(BigDecimal.valueOf(50000))
                    .currency("VND")
                    .status(AccountStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build());

            // Destination active account with 10,000,000 VND balance
            accountRepository.save(BankAccount.builder()
                    .accountNumber("1000000002")
                    .customerId(2L)
                    .accountType("CHECKING")
                    .balance(BigDecimal.valueOf(10000000))
                    .minimumBalance(BigDecimal.valueOf(50000))
                    .currency("VND")
                    .status(AccountStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .build());

            // Locked account for testing Saga rollback scenario!
            accountRepository.save(BankAccount.builder()
                    .accountNumber("1000000003")
                    .customerId(2L)
                    .accountType("SAVINGS")
                    .balance(BigDecimal.valueOf(5000000))
                    .minimumBalance(BigDecimal.valueOf(1000000))
                    .currency("VND")
                    .status(AccountStatus.LOCKED)
                    .createdAt(Instant.now())
                    .build());

            log.info("Initialized test accounts: 1000000001 (Active), 1000000002 (Active), 1000000003 (Locked)");
        }
    }
}
