package com.rikkeibank.customer.config;

import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.AccountTypeRepository;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final AccountTypeRepository accountTypeRepository;

    @Override
    public void run(String... args) {
        if (accountTypeRepository.count() == 0) {
            log.info("Seeding Account Types catalog...");
            accountTypeRepository.save(AccountType.builder()
                    .typeCode("CHECKING")
                    .typeName("Standard Checking Account")
                    .description("Daily payment and transfer account with 0.1% interest")
                    .interestRate(BigDecimal.valueOf(0.1))
                    .minimumBalance(BigDecimal.valueOf(50000))
                    .active(true)
                    .build());

            accountTypeRepository.save(AccountType.builder()
                    .typeCode("SAVINGS")
                    .typeName("High Yield Savings Account")
                    .description("Savings account with 6.5% yearly interest")
                    .interestRate(BigDecimal.valueOf(6.5))
                    .minimumBalance(BigDecimal.valueOf(1000000))
                    .active(true)
                    .build());

            accountTypeRepository.save(AccountType.builder()
                    .typeCode("GOLD")
                    .typeName("RikkeiBank Gold Priority Account")
                    .description("Exclusive priority account with free wire transfers")
                    .interestRate(BigDecimal.valueOf(1.0))
                    .minimumBalance(BigDecimal.valueOf(50000000))
                    .active(true)
                    .build());
        }

        if (customerRepository.count() == 0) {
            log.info("Seeding initial Customer profile...");
            customerRepository.save(Customer.builder()
                    .userId(3L) // Matches user 'customer1'
                    .customerCode("CUST-001")
                    .fullName("Tran Van Customer")
                    .idCardNumber("001200001234")
                    .phoneNumber("0987654321")
                    .email("customer1@gmail.com")
                    .address("123 Duy Tan, Cau Giay, Hanoi")
                    .dateOfBirth(LocalDate.of(1995, 5, 20))
                    .active(true)
                    .createdAt(Instant.now())
                    .build());

            // Add a second customer for transfer testing
            customerRepository.save(Customer.builder()
                    .userId(4L)
                    .customerCode("CUST-002")
                    .fullName("Le Thi Beneficiary")
                    .idCardNumber("001200005678")
                    .phoneNumber("0912345678")
                    .email("beneficiary@gmail.com")
                    .address("456 Le Van Luong, Thanh Xuan, Hanoi")
                    .dateOfBirth(LocalDate.of(1998, 8, 15))
                    .active(true)
                    .createdAt(Instant.now())
                    .build());
        }

        if (staffRepository.count() == 0) {
            log.info("Seeding initial Staff profile...");
            staffRepository.save(Staff.builder()
                    .userId(2L) // Matches user 'teller1'
                    .staffCode("STAFF-001")
                    .fullName("Nguyen Van Teller")
                    .branch("Hanoi Main Branch")
                    .position("TELLER")
                    .email("teller1@rikkeibank.com")
                    .phoneNumber("0901234567")
                    .active(true)
                    .createdAt(Instant.now())
                    .build());
        }
    }
}
