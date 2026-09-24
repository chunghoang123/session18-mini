package com.rikkeibank.common.events;

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
public class TransferFailedEvent implements Serializable {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String reason;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
