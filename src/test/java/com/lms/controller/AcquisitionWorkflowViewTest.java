package com.lms.controller;

import com.lms.entity.MemberAccount;
import com.lms.repository.BookRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.impl.CustomMemberDetailsService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class AcquisitionWorkflowViewTest {

    @Autowired MockMvc mockMvc;
    @Autowired BookRepository bookRepository;
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
    void rendersStandardizedMemberInteractionPagesWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        MemberAccount account = accounts.get(0);
        var memberUser = memberDetailsService.loadUserByUsername(account.getUsername());

        mockMvc.perform(get("/member/favorites").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/favorites"));
        mockMvc.perform(get("/member/interaction/reviews").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/reviews"));
        mockMvc.perform(get("/member/interaction/acquisition-requests/new").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/book-acquisition-request"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-target=\"#acquisitionRequestModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"acquisitionRequestModal\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"acquisitionRequestForm\"")));
        mockMvc.perform(get("/member/interaction/notifications").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/notifications"));
        mockMvc.perform(get("/member/interaction/notifications")
                        .param("source", "librarian")
                        .param("type", "RESERVATION")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/notifications"))
                .andExpect(model().attribute("selectedNotificationSource", "librarian"))
                .andExpect(model().attribute("selectedNotificationType", "RESERVATION"));
        mockMvc.perform(get("/member/borrow/management")
                        .param("tab", "borrowing")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/borrow"))
                .andExpect(model().attribute("activeTab", "borrowing"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("member-loan-hero")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"renewConfirmModal\"")));
        mockMvc.perform(get("/member/borrow/management")
                        .param("tab", "reserved")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/borrow"))
                .andExpect(model().attribute("activeTab", "reserved"));
        mockMvc.perform(get("/member/borrow/management")
                        .param("tab", "history")
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/borrow"))
                .andExpect(model().attribute("activeTab", "history"));
        var availableBook = bookRepository.findAll().stream()
                .filter(book -> !"Inactive".equalsIgnoreCase(book.getStatus()))
                .findFirst();
        Assumptions.assumeTrue(availableBook.isPresent(), "Current database has no active book");
        mockMvc.perform(get("/member/borrow/create")
                        .param("bookId", String.valueOf(availableBook.get().getBookId()))
                        .with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/borrow-create"))
                .andExpect(model().attributeExists("selectedBook", "selectedBookId", "maxBorrowDays"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("member-borrow-create-hero")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/member/borrow/request/submit\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"numberOfDays\"")));
        mockMvc.perform(get("/member/membership/tier").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/membership-tier"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("??"))));
        mockMvc.perform(get("/member/membership/benefits").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/membership-benefits"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("??"))));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianOperationalDashboard() throws Exception {
        mockMvc.perform(get("/librarian/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tasks Awaiting Action")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianAcquisitionDashboardWithCurrentDatabase() throws Exception {
        mockMvc.perform(get("/librarian/dashboard").param("section", "acquisition"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/librarian/interaction/acquisition-requests"));

        var result = mockMvc.perform(get("/librarian/interaction/acquisition-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/acquisition-request-list"))
                .andReturn();

        Page<?> requests = (Page<?>) result.getModelAndView().getModel().get("requests");
        assertThat(requests.getSort().getOrderFor("createdDate").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(requests.getSort().getOrderFor("requestId").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
