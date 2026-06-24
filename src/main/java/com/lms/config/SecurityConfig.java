package com.lms.config;


import com.lms.service.AuthService;
import com.lms.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Cấu hình Spring Security
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthService authService;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(AuthService authService, CustomOAuth2UserService customOAuth2UserService) {
        this.authService = authService;
        this.customOAuth2UserService = customOAuth2UserService;
    }
    // SecurityConfig chỉ tập trung đúng chuyên môn là "Bảo vệ các đường dẫn (URL)"

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // TODO: Có thể bật CSRF lên và xử lý token nên xóa khi hoàn thành dự án .disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/books/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/librarian/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers("/member/**").hasRole("MEMBER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                            Integer accountId = userDetails.getAccount().getAccountId();
                            String ipAddress = request.getRemoteAddr(); // lấy thông tin tên đầy đủ của khách hàng hoạc máy chủ proxy
                            String userAgent = request.getHeader("User-Agent");
                            String ssesionId = request.getSession().getId();
                            authService.logLoginAction(accountId, ipAddress, userAgent, ssesionId);
                            response.sendRedirect("/");
                        })
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler((request, response, authentication) -> {
                            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                            Integer accountId = userDetails.getAccount().getAccountId();
                            String ipAddress = request.getRemoteAddr();
                            String userAgent = request.getHeader("User-Agent");
                            String sessionId = request.getSession().getId();
                            authService.logLoginAction(accountId, ipAddress, userAgent, sessionId);
                            response.sendRedirect("/");
                        })
                )

                .logout(logout -> logout
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Khi đăng xuất, có khi session đã hết hạn từ trước nên 'authentication' bị null.
                            //tránh lỗi NullPointerException (văng app).
                            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                                Integer accountId = userDetails.getAccount().getAccountId();
                                String ipAddress = request.getRemoteAddr();
                                String userAgent = request.getHeader("User-Agent");
                                String sessionId = request.getSession().getId();
                                authService.logLogoutAction(accountId, ipAddress, userAgent, sessionId);
                                response.sendRedirect("/login?logout");
                            }
                        })
                        .permitAll()
                )
                .sessionManagement(session -> session
                                // TODO: Cấu hình SessionManagement: tối đa 1 phiên/user, nếu đăng nhập nơi khác thì kick ra (hoặc chặn)
                                .maximumSessions(1)
                                .expiredUrl("/login?expired")
                        // TODO: Cấu hình SessionRegistryBean để có thể lấy danh sách thiết bị đang đăng nhập
                );

        return http.build();
    }
}
