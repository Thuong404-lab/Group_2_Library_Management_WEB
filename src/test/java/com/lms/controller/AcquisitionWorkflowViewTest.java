package com.lms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AcquisitionWorkflowViewTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithUserDetails(value = "qanh123", userDetailsServiceBeanName = "customMemberDetailsService")
    void rendersMemberAcquisitionPageWithCurrentDatabase() throws Exception {
        mockMvc.perform(get("/member/interaction/acquisition-requests/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/book-acquisition-request"));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianAcquisitionDashboardWithCurrentDatabase() throws Exception {
        mockMvc.perform(get("/librarian/dashboard").param("section", "acquisition"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/dashboard"));
    }
}
