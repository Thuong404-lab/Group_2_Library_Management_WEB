package com.lms.config;

import com.lms.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Set;

/**
 * SecurityConfig - Spring Security Configuration
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard");
            } else if (roles.contains("ROLE_LIBRARIAN")) {
                response.sendRedirect("/librarian/dashboard");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // TODO: Có thể bật CSRF lên và xử lý token sau khi hoàn thành dự án
                .authorizeHttpRequests(auth -> auth
                        // 1. Các URL công khai ai cũng được vào (Gộp đầy đủ cả 2 nhánh)
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/books/**", "/about",
                                "/member/membership/benefits", "/member/membership/tier", "/librarian/borrow/create").permitAll()

                        // 2. Phân quyền nghiêm ngặt theo vai trò (Role-based Authorization)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/librarian/**").hasAnyRole("LIBRARIAN") // Cho cả ADMIN và LIBRARIAN truy cập hệ sinh thái này
                        .requestMatchers("/member/**").hasRole("MEMBER")
                        .anyRequest().authenticated()
                )

                //Quoc Anh đã sửa chỗ này ( Dùng method successHandler thay vì defaultSuccessUrl)
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customSuccessHandler())
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                        // TODO: Cấu hình LogoutSuccessHandler để ghi log vào bảng SystemLogs (AuthService.logLogoutAction)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        // TODO: Cấu hình SessionManagement: tối đa 1 phiên/user, nếu đăng nhập nơi khác thì kick ra (hoặc chặn)
                        .maximumSessions(1)
                        .expiredUrl("/login?expired")
                        .sessionRegistry(sessionRegistry())
                )
                // 3. XỬ LÝ NGOẠI LỆ: Khi sai quyền, tự động đá về trang /403
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/403")
                );

        return http.build();
    }
}