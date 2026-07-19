package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    public static final String LOCALE_COOKIE_NAME = "LMS_LOCALE";
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
        CookieLocaleResolver resolver = new CookieLocaleResolver(LOCALE_COOKIE_NAME);
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
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
