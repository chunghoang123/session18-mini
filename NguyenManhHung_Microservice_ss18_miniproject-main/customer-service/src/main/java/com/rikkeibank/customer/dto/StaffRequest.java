package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRequest {
    private Long userId;

    @NotBlank(message = "Staff code is required")
    private String staffCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Branch is required")
    private String branch;

    @NotBlank(message = "Position is required")
    private String position;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private Boolean active;
}
