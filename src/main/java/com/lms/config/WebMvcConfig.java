package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import com.lms.config.InactiveMemberInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private InactiveMemberInterceptor inactiveMemberInterceptor;

    private final MemberLocalePreferenceInterceptor memberLocalePreferenceInterceptor;

    public WebMvcConfig() {
        this.memberLocalePreferenceInterceptor = null;
    }

    @Autowired
    public WebMvcConfig(MemberLocalePreferenceInterceptor memberLocalePreferenceInterceptor) {
        this.memberLocalePreferenceInterceptor = memberLocalePreferenceInterceptor;
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(inactiveMemberInterceptor).addPathPatterns("/member/**", "/api/**");
        if (memberLocalePreferenceInterceptor != null) {
            registry.addInterceptor(memberLocalePreferenceInterceptor);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Map đường dẫn /uploads/** tới thư mục vật lý trên máy
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}
