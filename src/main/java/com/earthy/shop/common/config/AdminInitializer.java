package com.earthy.shop.common.config;

import com.earthy.shop.domain.admin.entity.Admin;
import com.earthy.shop.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initAdmin() {
        return args -> {
            if (adminRepository.existsByEmail(adminEmail)) {
                return;
            }

            Admin admin = new Admin(
                    adminEmail,
                    passwordEncoder.encode(adminPassword)
            );

            adminRepository.save(admin);
        };
    }
}