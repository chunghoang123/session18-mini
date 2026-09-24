package com.rikkeibank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditRequest {

    @NotNull(message = "Credit amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum transaction amount is 1,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private String reason;
}
