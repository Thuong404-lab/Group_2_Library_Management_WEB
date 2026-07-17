package com.lms.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebMvcConfigTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WebMvcConfig config = new WebMvcConfig();
        mockMvc = MockMvcBuilders.standaloneSetup(new LocaleProbeController())
                .setLocaleResolver(config.localeResolver())
                .addInterceptors(config.localeChangeInterceptor())
                .build();
    }

    @Test
    void usesEnglishByDefault() throws Exception {
        mockMvc.perform(get("/test/locale"))
                .andExpect(status().isOk())
                .andExpect(content().string("en"));
    }

    @Test
    void changesLocaleToVietnameseAndPersistsItInCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/locale").param("lang", "vi"))
                .andExpect(status().isOk())
                .andExpect(content().string("vi"))
                .andReturn();

        Cookie localeCookie = result.getResponse().getCookie(WebMvcConfig.LOCALE_COOKIE_NAME);
        assertThat(localeCookie).isNotNull();
        assertThat(localeCookie.getValue()).isEqualTo("vi");

        mockMvc.perform(get("/test/locale").cookie(localeCookie))
                .andExpect(status().isOk())
                .andExpect(content().string("vi"));
    }

    @RestController
    private static class LocaleProbeController {

        @GetMapping("/test/locale")
        String locale() {
            return LocaleContextHolder.getLocale().getLanguage();
        }
    }
}
