package com.rikkeibank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse implements Serializable {
    private Long id;
    private Long userId;
    private String customerCode;
    private String fullName;
    private String idCardNumber;
    private String phoneNumber;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    private boolean active;
    private Instant createdAt;
}
