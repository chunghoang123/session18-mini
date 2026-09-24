package com.rikkeibank.transaction.dto;

import com.rikkeibank.common.enums.TransactionStatus;
import com.rikkeibank.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse implements Serializable {
    private Long id;
    private String transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String note;
    private String failureReason;
    private Long tellerId;
    private String initiatedBy;
    private Instant createdAt;
    private Instant completedAt;
}
