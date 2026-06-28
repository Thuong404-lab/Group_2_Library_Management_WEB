package com.lms.controller.auth;

import com.lms.config.SecurityConfig;
import com.lms.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void anonymousUserCanOpenForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("forgotPasswordRequest"));
    }

    @Test
    void loginPageLinksToForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/forgot-password\"")));
    }

    @Test
    void invalidEmailDoesNotCreateResetRequest() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "invalid-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeHasFieldErrors("forgotPasswordRequest", "email"));

        verify(authService, never()).requestPasswordReset("invalid-email");
    }

    @Test
    void resetPageBindsTokenToForm() throws Exception {
        String token = "73967be5-08d6-43a7-8fe1-4cd94311c877";

        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("resetPasswordRequest",
                        org.hamcrest.Matchers.hasProperty("token", org.hamcrest.Matchers.is(token))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "value=\"" + token + "\"")));

        verify(authService).validatePasswordResetToken(token);
    }

    @Test
    void mismatchedPasswordsReturnFieldErrorWithoutResettingPassword() throws Exception {
        String token = "73967be5-08d6-43a7-8fe1-4cd94311c877";

        mockMvc.perform(post("/reset-password")
                        .param("token", token)
                        .param("newPassword", "newPassword")
                        .param("confirmPassword", "differentPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attributeHasFieldErrors(
                        "resetPasswordRequest", "confirmPassword"));

        verify(authService, never()).resetPassword(token, "newPassword");
    }

    @Test
    void validResetChangesPasswordAndRedirectsToLogin() throws Exception {
        String token = "73967be5-08d6-43a7-8fe1-4cd94311c877";

        mockMvc.perform(post("/reset-password")
                        .param("token", token)
                        .param("newPassword", "newPassword")
                        .param("confirmPassword", "newPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMsg"));

        verify(authService).resetPassword(token, "newPassword");
    }
}
