package com.rikkeibank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeResponse implements Serializable {
    private Long id;
    private String typeCode;
    private String typeName;
    private String description;
    private BigDecimal interestRate;
    private BigDecimal minimumBalance;
    private boolean active;
}
