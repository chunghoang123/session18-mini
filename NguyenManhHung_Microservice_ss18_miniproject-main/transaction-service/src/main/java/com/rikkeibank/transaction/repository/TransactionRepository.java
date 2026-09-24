package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByTransactionId(String transactionId);

    Page<TransactionRecord> findByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(
            String fromAccountNumber, String toAccountNumber, Pageable pageable);

    Page<TransactionRecord> findByTellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long tellerId, Instant start, Instant end, Pageable pageable);

    Page<TransactionRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant start, Instant end, Pageable pageable);
}
