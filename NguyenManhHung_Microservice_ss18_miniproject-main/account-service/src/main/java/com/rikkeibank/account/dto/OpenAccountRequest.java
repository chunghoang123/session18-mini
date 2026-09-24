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
public class OpenAccountRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Account type is required")
    private String accountType; // CHECKING, SAVINGS, GOLD

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "50000.0", message = "Initial deposit must be at least 50,000 VND")
    private BigDecimal initialDeposit;

    private String currency;
}
