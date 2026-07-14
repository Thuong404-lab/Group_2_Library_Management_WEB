package com.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // 1. Khai báo thuộc tính Interceptor để hết báo đỏ lỗi biên dịch
    private final CartInterceptor cartInterceptor;

    // 2. Tạo Constructor để Spring Boot tự động Inject Bean
    public WebMvcConfig(CartInterceptor cartInterceptor) {
        this.cartInterceptor = cartInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Map đường dẫn /uploads/** tới thư mục vật lý trên máy
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Áp dụng tính toán giỏ sách toàn bộ khu vực thành viên
        registry.addInterceptor(cartInterceptor);
    }
}