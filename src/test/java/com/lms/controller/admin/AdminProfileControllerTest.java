package com.lms.controller.admin;

import com.lms.entity.User;
import com.lms.exception.ConflictException;
import com.lms.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProfileControllerTest {

    @Mock ProfileService profileService;
    @InjectMocks AdminProfileController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateProfileDoesNotRequireOrUpdateEmail() {
        Principal principal = () -> "admin";
        User current = new User();
        current.setEmail("admin@example.com");
        when(profileService.getStaffProfile("admin")).thenReturn(current);
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        String view = controller.updateProfile("System Administrator", "0900000001", null, principal, flash);

        assertThat(view).isEqualTo("redirect:/admin/profile");
        verify(profileService).updateStaffProfile("admin", "System Administrator", "0900000001", null);
        assertThat(flash.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void phoneConflictIsReturnedToThePhoneField() {
        Principal principal = () -> "admin";
        User current = new User();
        current.setEmail("admin@example.com");
        when(profileService.getStaffProfile("admin")).thenReturn(current);
        doThrow(new ConflictException("phone", "Phone already used"))
                .when(profileService).updateStaffProfile("admin", "System Administrator", "0900000001", null);
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        controller.updateProfile("System Administrator", "0900000001", null, principal, flash);

        assertThat(flash.getFlashAttributes().get("phoneError")).isEqualTo("Phone already used");
        assertThat(flash.getFlashAttributes()).containsKey("admin");
    }
}
