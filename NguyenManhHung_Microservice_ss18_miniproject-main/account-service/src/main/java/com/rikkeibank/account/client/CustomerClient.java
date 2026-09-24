package com.rikkeibank.account.client;

import com.rikkeibank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "customer-service", fallback = CustomerClientFallback.class)
public interface CustomerClient {

    @GetMapping("/api/v1/customers/{id}")
    ApiResponse<Map<String, Object>> getCustomerById(@PathVariable("id") Long id);
}
