package com.rikkeibank.customer.service;

import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.customer.dto.AccountTypeRequest;
import com.rikkeibank.customer.dto.AccountTypeResponse;
import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    @Transactional
    @CacheEvict(value = {"accountTypes", "accountTypeList"}, allEntries = true)
    public AccountTypeResponse createAccountType(AccountTypeRequest request) {
        if (accountTypeRepository.existsByTypeCode(request.getTypeCode())) {
            throw new AppException(ErrorCode.CONFLICT, "Account type code already exists: " + request.getTypeCode());
        }

        AccountType type = AccountType.builder()
                .typeCode(request.getTypeCode().toUpperCase())
                .typeName(request.getTypeName())
                .description(request.getDescription())
                .interestRate(request.getInterestRate())
                .minimumBalance(request.getMinimumBalance())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        AccountType saved = accountTypeRepository.save(type);
        log.info("Created account type: code={}, name={}", saved.getTypeCode(), saved.getTypeName());
        return mapToResponse(saved);
    }

    @Cacheable(value = "accountTypes", key = "#typeCode")
    public AccountTypeResponse getAccountTypeByCode(String typeCode) {
        log.info("Fetching account type from DB for code: {}", typeCode);
        AccountType type = accountTypeRepository.findByTypeCode(typeCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Account type not found: " + typeCode));
        return mapToResponse(type);
    }

    @Cacheable(value = "accountTypeList")
    public List<AccountTypeResponse> getAllAccountTypes() {
        log.info("Fetching all account types from DB");
        return accountTypeRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    @CacheEvict(value = {"accountTypes", "accountTypeList"}, allEntries = true)
    public AccountTypeResponse updateAccountType(Long id, AccountTypeRequest request) {
        AccountType type = accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account type not found with id: " + id));

        type.setTypeName(request.getTypeName());
        type.setDescription(request.getDescription());
        if (request.getInterestRate() != null) {
            type.setInterestRate(request.getInterestRate());
        }
        if (request.getMinimumBalance() != null) {
            type.setMinimumBalance(request.getMinimumBalance());
        }
        if (request.getActive() != null) {
            type.setActive(request.getActive());
        }

        AccountType updated = accountTypeRepository.save(type);
        log.info("Updated account type: id={}, code={}", updated.getId(), updated.getTypeCode());
        return mapToResponse(updated);
    }

    @Transactional
    @CacheEvict(value = {"accountTypes", "accountTypeList"}, allEntries = true)
    public void deleteAccountType(Long id) {
        AccountType type = accountTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account type not found with id: " + id));
        accountTypeRepository.delete(type);
        log.info("Deleted account type: id={}", id);
    }

    private AccountTypeResponse mapToResponse(AccountType type) {
        return AccountTypeResponse.builder()
                .id(type.getId())
                .typeCode(type.getTypeCode())
                .typeName(type.getTypeName())
                .description(type.getDescription())
                .interestRate(type.getInterestRate())
                .minimumBalance(type.getMinimumBalance())
                .active(type.isActive())
                .build();
    }
}
