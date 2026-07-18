package com.lms.config;

import com.lms.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class I18nPublicViewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void loginPageUsesEnglishByDefault() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Log In")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Discover Knowledge")));
    }

    @Test
    void loginPageCanSwitchToVietnamese() throws Exception {
        mockMvc.perform(get("/login").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(cookie().value(WebMvcConfig.LOCALE_COOKIE_NAME, "vi"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng Nhập")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khám phá Tri Thức")));
    }

    @Test
    void catalogPageUsesEnglishByDefault() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Apply Filters")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sort by:")));
    }

    @Test
    void membershipTierPageUsesEnglishByDefault() throws Exception {
        mockMvc.perform(get("/membership-tiers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Explore membership tiers")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tier requirement")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Điều kiện đạt hạng"))));
    }

    @Test
    void membershipTierPageCanSwitchToVietnamese() throws Exception {
        mockMvc.perform(get("/membership-tiers").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(cookie().value(WebMvcConfig.LOCALE_COOKIE_NAME, "vi"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khám phá các hạng thành viên")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Điều kiện đạt hạng")));
    }

    @Test
    void bookDetailUsesEnglishByDefault() throws Exception {
        var books = bookRepository.findAll();
        org.junit.jupiter.api.Assumptions.assumeFalse(books.isEmpty(), "Current database has no books");

        mockMvc.perform(get("/books/{id}", books.get(0).getBookId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Book Description")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reader Reviews")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Đánh giá từ bạn đọc"))));
    }

    @Test
    void adminDashboardUsesEnglishByDefault() throws Exception {
                mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Risk Overview")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("System Log")));
    }

    @Test
    void adminSettingsCanSwitchToVietnamese() throws Exception {
        mockMvc.perform(get("/admin/settings")
                        .param("lang", "vi")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(cookie().value(WebMvcConfig.LOCALE_COOKIE_NAME, "vi"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cài đặt hệ thống")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu cấu hình")));
    }
}
