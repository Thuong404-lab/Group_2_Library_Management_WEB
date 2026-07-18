package com.lms.controller;

import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.impl.CustomMemberDetailsService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class AcquisitionWorkflowViewTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberAccountRepository memberAccountRepository;
    @Autowired CustomMemberDetailsService memberDetailsService;

    @Test
    void rendersMemberAcquisitionPageWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        MemberAccount account = accounts.get(0);
        var memberUser = memberDetailsService.loadUserByUsername(account.getUsername());

        mockMvc.perform(get("/member/interaction/acquisition-requests/new").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/book-acquisition-request"));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianAcquisitionDashboardWithCurrentDatabase() throws Exception {
        mockMvc.perform(get("/librarian/dashboard").param("section", "acquisition"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/librarian/interaction/acquisition-requests"));

        mockMvc.perform(get("/librarian/interaction/acquisition-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/acquisition-request-list"));
    }
}
