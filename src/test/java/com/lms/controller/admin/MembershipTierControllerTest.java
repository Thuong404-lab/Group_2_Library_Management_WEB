package com.lms.controller.admin;

import com.lms.service.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipTierControllerTest {
    private MembershipService membershipService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        membershipService = mock(MembershipService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MembershipTierController(membershipService)).build();
    }

    @Test
    void malformedNumberRedirectsBackToModalInsteadOfRenderingErrorPage() throws Exception {
        mockMvc.perform(post("/admin/tiers/save")
                        .param("tierId", "1")
                        .param("version", "0")
                        .param("condition", "0")
                        .param("discountPercent", "not-a-number")
                        .param("borrowLimit", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tiers"))
                .andExpect(flash().attribute("openTierId", 1))
                .andExpect(flash().attributeExists("tierFieldErrors", "error"));

        verifyNoInteractions(membershipService);
    }
}
