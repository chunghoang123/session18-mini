package com.rikkeibank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "account_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", unique = true, nullable = false, length = 30)
    private String typeCode; // CHECKING, SAVINGS, BUSINESS, GOLD

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(length = 255)
    private String description;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "minimum_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minimumBalance = BigDecimal.valueOf(50000); // 50,000 VND

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
