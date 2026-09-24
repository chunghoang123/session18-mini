package com.rikkeibank.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse implements Serializable {
    private Long id;
    private Long userId;
    private String staffCode;
    private String fullName;
    private String branch;
    private String position;
    private String email;
    private String phoneNumber;
    private boolean active;
    private Instant createdAt;
}
