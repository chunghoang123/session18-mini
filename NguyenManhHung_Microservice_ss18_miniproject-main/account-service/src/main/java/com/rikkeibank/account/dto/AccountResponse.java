package com.rikkeibank.account.dto;

import com.rikkeibank.common.enums.AccountStatus;
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
public class AccountResponse implements Serializable {
    private Long id;
    private String accountNumber;
    private Long customerId;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal minimumBalance;
    private String currency;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
