package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Cấu hình Spring Security
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO: Implement - Cấu hình phân quyền
        // TODO: /admin/** → chỉ ROLE_ADMIN
        // TODO: /librarian/** → chỉ ROLE_LIBRARIAN hoặc ROLE_ADMIN
        // TODO: /member/** → chỉ ROLE_MEMBER
        // TODO: /, /books/**, /login, /register → permitAll

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/books/**", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                // TODO: Thêm các quy tắc phân quyền ở đây
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
