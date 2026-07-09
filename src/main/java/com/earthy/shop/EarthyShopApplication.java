package com.earthy.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA Auditing 활성화
@EnableJpaAuditing
@SpringBootApplication
public class EarthyShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(EarthyShopApplication.class, args);
    }
}
