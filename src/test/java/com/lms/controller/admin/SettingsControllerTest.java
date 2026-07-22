package com.lms.controller.admin;

import com.lms.service.SystemService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsControllerTest {

    @Test
    void policyUpdateAcceptsCurrentSettingsFields() throws Exception {
        SystemService systemService = mock(SystemService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new SettingsController(systemService))
                .build();

        mockMvc.perform(post("/admin/settings/policies")
                        .param("maxBorrowDays", "14")
                        .param("maxRenewalDays", "7")
                        .param("maxRenewalRequests", "2")
                        .param("renewalRejectionCooldownHours", "24")
                        .param("renewalApprovalTimeoutHours", "12")
                        .param("borrowFeePerBook", "5000")
                        .param("finePerDay", "5000")
                        .param("damageCompensationAmount", "120000")
                        .param("damageCompensationThreshold", "50")
                        .param("overdueViolationLockLimit", "3")
                        .param("depositAmount", "50000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/settings"));

        verify(systemService).updateBorrowingPolicies(
                14,
                7,
                2,
                24,
                12,
                new BigDecimal("5000"),
                new BigDecimal("5000"),
                new BigDecimal("120000"),
                50,
                3,
                new BigDecimal("50000"));
    }
}
