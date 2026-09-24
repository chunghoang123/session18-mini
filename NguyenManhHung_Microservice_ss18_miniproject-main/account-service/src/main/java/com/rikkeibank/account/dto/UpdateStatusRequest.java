package com.rikkeibank.account.dto;

import com.rikkeibank.common.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    @NotNull(message = "Account status is required")
    private AccountStatus status;
}
