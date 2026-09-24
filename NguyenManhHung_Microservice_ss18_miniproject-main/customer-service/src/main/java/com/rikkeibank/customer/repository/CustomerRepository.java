package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCode(String customerCode);
    Optional<Customer> findByUserId(Long userId);
    boolean existsByCustomerCode(String customerCode);
    boolean existsByIdCardNumber(String idCardNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
}
