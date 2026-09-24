package com.rikkeibank.transaction.client;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.transaction.client.dto.AccountActionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    ApiResponse<Map<String, Object>> getAccount(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/v1/accounts/{accountNumber}/debit")
    ApiResponse<Map<String, Object>> debit(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountActionRequest request);

    @PostMapping("/api/v1/accounts/{accountNumber}/credit")
    ApiResponse<Map<String, Object>> credit(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountActionRequest request);

    @PostMapping("/api/v1/accounts/{accountNumber}/compensate-debit")
    ApiResponse<Map<String, Object>> compensateDebit(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody AccountActionRequest request);
}
