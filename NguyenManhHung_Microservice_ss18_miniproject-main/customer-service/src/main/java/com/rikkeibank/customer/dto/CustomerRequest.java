package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {
    private Long userId;

    @NotBlank(message = "Customer code is required")
    private String customerCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "ID card number is required")
    private String idCardNumber;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String address;
    private LocalDate dateOfBirth;
    private Boolean active;
}
