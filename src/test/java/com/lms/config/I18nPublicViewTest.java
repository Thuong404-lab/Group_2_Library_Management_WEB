package com.lms.config;

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
    void adminDashboardUsesEnglishByDefault() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Overview Statistics")))
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
