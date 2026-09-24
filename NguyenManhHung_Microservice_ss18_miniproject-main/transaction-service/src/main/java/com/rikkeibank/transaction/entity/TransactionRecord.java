package com.rikkeibank.transaction.entity;

import com.rikkeibank.common.enums.TransactionStatus;
import com.rikkeibank.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 64)
    private String transactionId;

    @Column(name = "from_account_number", nullable = false, length = 20)
    private String fromAccountNumber;

    @Column(name = "to_account_number", nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Builder.Default
    private TransactionType transactionType = TransactionType.TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 255)
    private String note;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "teller_id")
    private Long tellerId;

    @Column(name = "initiated_by", length = 50)
    private String initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
