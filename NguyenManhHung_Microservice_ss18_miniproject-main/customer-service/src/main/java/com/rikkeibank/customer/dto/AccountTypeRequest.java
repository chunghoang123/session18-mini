package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeRequest {

    @NotBlank(message = "Type code is required")
    private String typeCode;

    @NotBlank(message = "Type name is required")
    private String typeName;

    private String description;

    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    private BigDecimal interestRate;

    @DecimalMin(value = "0.0", message = "Minimum balance cannot be negative")
    private BigDecimal minimumBalance;

    private Boolean active;
}
