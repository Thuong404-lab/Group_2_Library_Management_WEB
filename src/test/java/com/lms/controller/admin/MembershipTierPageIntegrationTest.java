package com.lms.controller.admin;

import com.lms.entity.MembershipTier;
import com.lms.service.MembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class MembershipTierPageIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MembershipService membershipService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rendersHeroTableAndEditModal() throws Exception {
        MembershipTier tier = new MembershipTier(1, "Member", BigDecimal.ZERO, 5,
                BigDecimal.ZERO, "Borrow up to 5 books concurrently.");
        tier.setVersion(0L);
        when(membershipService.getAllTiers()).thenReturn(List.of(tier));
        when(membershipService.getMemberCountByTier()).thenReturn(Map.of(1, 22L));

        mockMvc.perform(get("/admin/tiers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-tiers"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"tiers-hero\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"editTierModal-1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\" value=\"0\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"benefits\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("class=\"info-sub benefit-description\""))));
    }

    @Test
    void publicTierCardsDoNotRepeatHardCodedBenefitDetails() throws Exception {
        MembershipTier tier = new MembershipTier(1, "Member", BigDecimal.ZERO, 5,
                BigDecimal.ZERO, "Borrow up to 5 books concurrently.");
        when(membershipService.getAllTiers()).thenReturn(List.of(tier));

        mockMvc.perform(get("/membership-tiers"))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/membership-tiers"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("membership-tier-benefits"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Borrow up to 5 books concurrently."))));
    }
}
