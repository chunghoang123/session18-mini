package com.rikkeibank.customer.service;

import com.rikkeibank.common.dto.PageResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.exception.ResourceNotFoundException;
import com.rikkeibank.customer.dto.StaffRequest;
import com.rikkeibank.customer.dto.StaffResponse;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    @Transactional
    public StaffResponse createStaff(StaffRequest request) {
        if (staffRepository.existsByStaffCode(request.getStaffCode())) {
            throw new AppException(ErrorCode.CONFLICT, "Staff code already exists: " + request.getStaffCode());
        }
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS, "Email already exists: " + request.getEmail());
        }
        if (staffRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.CONFLICT, "Phone number already exists: " + request.getPhoneNumber());
        }

        Staff staff = Staff.builder()
                .userId(request.getUserId())
                .staffCode(request.getStaffCode())
                .fullName(request.getFullName())
                .branch(request.getBranch())
                .position(request.getPosition())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(Instant.now())
                .build();

        Staff saved = staffRepository.save(staff);
        log.info("Created staff: id={}, code={}, position={}", saved.getId(), saved.getStaffCode(), saved.getPosition());
        return mapToResponse(saved);
    }

    public StaffResponse getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STAFF_NOT_FOUND, "Staff not found with id: " + id));
        return mapToResponse(staff);
    }

    public StaffResponse getStaffByCode(String code) {
        Staff staff = staffRepository.findByStaffCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STAFF_NOT_FOUND, "Staff not found with code: " + code));
        return mapToResponse(staff);
    }

    public List<StaffResponse> getStaffByBranch(String branch) {
        return staffRepository.findByBranch(branch).stream().map(this::mapToResponse).toList();
    }

    public PageResponse<StaffResponse> getAllStaff(int page, int size) {
        Page<Staff> staffPage = staffRepository.findAll(PageRequest.of(page, size));
        return PageResponse.<StaffResponse>builder()
                .content(staffPage.getContent().stream().map(this::mapToResponse).toList())
                .pageNumber(staffPage.getNumber())
                .pageSize(staffPage.getSize())
                .totalElements(staffPage.getTotalElements())
                .totalPages(staffPage.getTotalPages())
                .last(staffPage.isLast())
                .build();
    }

    @Transactional
    public StaffResponse updateStaff(Long id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STAFF_NOT_FOUND, "Staff not found with id: " + id));

        staff.setFullName(request.getFullName());
        staff.setBranch(request.getBranch());
        staff.setPosition(request.getPosition());
        staff.setEmail(request.getEmail());
        staff.setPhoneNumber(request.getPhoneNumber());
        if (request.getActive() != null) {
            staff.setActive(request.getActive());
        }

        Staff updated = staffRepository.save(staff);
        log.info("Updated staff: id={}, code={}", updated.getId(), updated.getStaffCode());
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STAFF_NOT_FOUND, "Staff not found with id: " + id));
        staffRepository.delete(staff);
        log.info("Deleted staff with id: {}", id);
    }

    private StaffResponse mapToResponse(Staff staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .userId(staff.getUserId())
                .staffCode(staff.getStaffCode())
                .fullName(staff.getFullName())
                .branch(staff.getBranch())
                .position(staff.getPosition())
                .email(staff.getEmail())
                .phoneNumber(staff.getPhoneNumber())
                .active(staff.isActive())
                .createdAt(staff.getCreatedAt())
                .build();
    }
}
