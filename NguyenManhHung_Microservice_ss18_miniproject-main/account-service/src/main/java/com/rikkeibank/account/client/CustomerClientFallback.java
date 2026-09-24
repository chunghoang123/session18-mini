package com.rikkeibank.account.client;

import com.rikkeibank.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CustomerClientFallback implements CustomerClient {

    @Override
    public ApiResponse<Map<String, Object>> getCustomerById(Long id) {
        log.warn("Fallback triggered: customer-service is temporarily unavailable for customerId: {}", id);
        return ApiResponse.error(503, "Customer service is temporarily unavailable");
    }
}
