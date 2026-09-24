package com.rikkeibank.customer.service;

import com.rikkeibank.common.dto.PageResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.customer.dto.CustomerRequest;
import com.rikkeibank.customer.dto.CustomerResponse;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByCustomerCode(request.getCustomerCode())) {
            throw new AppException(ErrorCode.CONFLICT, "Customer code already exists: " + request.getCustomerCode());
        }
        if (customerRepository.existsByIdCardNumber(request.getIdCardNumber())) {
            throw new AppException(ErrorCode.CONFLICT, "ID card number already exists: " + request.getIdCardNumber());
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.CONFLICT, "Phone number already exists: " + request.getPhoneNumber());
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS, "Email already exists: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .userId(request.getUserId())
                .customerCode(request.getCustomerCode())
                .fullName(request.getFullName())
                .idCardNumber(request.getIdCardNumber())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(Instant.now())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Created customer: id={}, code={}", saved.getId(), saved.getCustomerCode());
        return mapToResponse(saved);
    }

    @Cacheable(value = "customers", key = "#id")
    public CustomerResponse getCustomerById(Long id) {
        log.info("Fetching customer from database for id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with id: " + id));
        return mapToResponse(customer);
    }

    @Cacheable(value = "customersByCode", key = "#code")
    public CustomerResponse getCustomerByCode(String code) {
        log.info("Fetching customer from database for code: {}", code);
        Customer customer = customerRepository.findByCustomerCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with code: " + code));
        return mapToResponse(customer);
    }

    public PageResponse<CustomerResponse> getAllCustomers(int page, int size) {
        Page<Customer> customerPage = customerRepository.findAll(PageRequest.of(page, size));
        return PageResponse.<CustomerResponse>builder()
                .content(customerPage.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(customerPage.getNumber())
                .pageSize(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .last(customerPage.isLast())
                .build();
    }

    @Transactional
    @CachePut(value = "customers", key = "#id")
    @CacheEvict(value = "customersByCode", allEntries = true)
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with id: " + id));

        customer.setFullName(request.getFullName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getActive() != null) {
            customer.setActive(request.getActive());
        }

        Customer updated = customerRepository.save(customer);
        log.info("Updated customer: id={}, code={}", updated.getId(), updated.getCustomerCode());
        return mapToResponse(updated);
    }

    @Transactional
    @CacheEvict(value = {"customers", "customersByCode"}, allEntries = true)
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with id: " + id));
        customerRepository.delete(customer);
        log.info("Deleted customer with id: {}", id);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .userId(customer.getUserId())
                .customerCode(customer.getCustomerCode())
                .fullName(customer.getFullName())
                .idCardNumber(customer.getIdCardNumber())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .dateOfBirth(customer.getDateOfBirth())
                .active(customer.isActive())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
