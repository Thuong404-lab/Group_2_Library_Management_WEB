package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Security chain isolated to the signed PayOS webhook endpoint. */
@Configuration
public class PayOsSecurityConfig {
    @Bean
    @Order(0)
    SecurityFilterChain payOsWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/payments/payos/webhook")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
