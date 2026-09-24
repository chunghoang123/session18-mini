package com.rikkeibank.identity.config;

import com.rikkeibank.common.enums.RoleType;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial users for RikkeiBank identity-service...");

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@rikkeibank.com")
                    .fullName("System Administrator")
                    .role(RoleType.ROLE_ADMIN)
                    .enabled(true)
                    .createdAt(Instant.now())
                    .build();

            User teller = User.builder()
                    .username("teller1")
                    .password(passwordEncoder.encode("teller123"))
                    .email("teller1@rikkeibank.com")
                    .fullName("Nguyen Van Teller")
                    .role(RoleType.ROLE_TELLER)
                    .enabled(true)
                    .createdAt(Instant.now())
                    .build();

            User customer = User.builder()
                    .username("customer1")
                    .password(passwordEncoder.encode("customer123"))
                    .email("customer1@gmail.com")
                    .fullName("Tran Van Customer")
                    .role(RoleType.ROLE_CUSTOMER)
                    .enabled(true)
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
            userRepository.save(teller);
            userRepository.save(customer);

            log.info("Initialized default users: admin, teller1, customer1");
        }
    }
}
