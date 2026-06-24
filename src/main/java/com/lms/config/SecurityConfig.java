package com.lms.config;

// import com.lms.service.CustomOAuth2UserService;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

/**
 * SecurityConfig - Cấu hình Spring Security
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // @Autowired
    // private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // TODO: Có thể bật CSRF lên và xử lý token
                .authorizeHttpRequests(auth -> auth
                        // 1. Các URL công khai ai cũng được vào (Đã bỏ /librarian/profile ra khỏi đây)
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/books/**", "/member/membership/benefits", "/member/membership/tier").permitAll()

                        // 2. Phân quyền nghiêm ngặt theo vai trò (Role-based Authorization)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/librarian/**").hasRole("LIBRARIAN") // Chỉ LIBRARIAN mới được vào hệ sinh thái này
                        .requestMatchers("/member/**").hasRole("MEMBER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // TODO: Cấu hình AuthenticationSuccessHandler để ghi log vào bảng SystemLogs (AuthService.logLoginAction)
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                /*
                .oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                    )
                    .defaultSuccessUrl("/", true)
                )
                */
                .logout(logout -> logout
                        // TODO: Cấu hình LogoutSuccessHandler để ghi log vào bảng SystemLogs (AuthService.logLogoutAction)
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                                // TODO: Cấu hình SessionManagement: tối đa 1 phiên/user, nếu đăng nhập nơi khác thì kick ra (hoặc chặn)
                                .maximumSessions(1)
                                .expiredUrl("/login?expired")
                        // TODO: Cấu hình SessionRegistryBean để có thể lấy danh sách thiết bị đang đăng nhập
                )
                // 3. XỬ LÝ NGOẠI LỆ: Khi sai quyền (ví dụ Admin truy cập /librarian/profile), tự động đá về trang /403
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/403")
                );

        return http.build();
    }
}