package com.lms.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStaffModalViewTest {

    private static final Pattern UPDATE_TARGET =
            Pattern.compile("data-bs-target=\"(#updateStaffModal\\d+)\"");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void everyStaffEditTargetHasAMatchingModal() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/staff"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        Matcher targets = UPDATE_TARGET.matcher(html);
        boolean foundTarget = false;

        while (targets.find()) {
            foundTarget = true;
            String modalId = targets.group(1).substring(1);
            assertTrue(html.contains("id=\"" + modalId + "\""),
                    () -> "Missing modal element for " + modalId);
        }

        assertTrue(foundTarget, "No staff edit target was rendered");
        assertFalse(html.contains("href=\"#updateStaffModal"));
        assertFalse(html.contains("href=\"#deleteStaffModal"));
    }
}
