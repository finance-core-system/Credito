package com.credito.lending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.credito")
public class LendingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LendingServiceApplication.class, args);
    }
}
