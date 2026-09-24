package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByStaffCode(String staffCode);
    Optional<Staff> findByUserId(Long userId);
    List<Staff> findByBranch(String branch);
    boolean existsByStaffCode(String staffCode);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
