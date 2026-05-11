package com.credito.lending.config;

import com.credito.common.security.CreditoResourceServerProperties;
import com.credito.common.security.CreditoResourceServerSecurity;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(CreditoResourceServerProperties.class)
public class LendingSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CreditoResourceServerProperties properties
    ) throws Exception {
        return CreditoResourceServerSecurity.securityFilterChain(http, properties);
    }
}
