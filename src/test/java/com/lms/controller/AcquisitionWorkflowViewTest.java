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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
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
    void rendersMemberLoanManagementInEnglishWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        MemberAccount account = accounts.get(0);
        var memberUser = memberDetailsService.loadUserByUsername(account.getUsername());

        mockMvc.perform(get("/member/borrow/management").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/borrow"))
                .andExpect(content().string(containsString("Loan and Return Management")));
    }

    @Test
    void rendersMemberNotificationsInEnglishWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        MemberAccount account = accounts.get(0);
        var memberUser = memberDetailsService.loadUserByUsername(account.getUsername());

        mockMvc.perform(get("/member/interaction/notifications").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/notifications"))
                .andExpect(content().string(containsString("NOTIFICATION CENTER")));
    }

    @Test
    void rendersMemberNotificationsInVietnameseWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        var memberUser = memberDetailsService.loadUserByUsername(accounts.get(0).getUsername());

        mockMvc.perform(get("/member/interaction/notifications").param("lang", "vi").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/notifications"))
                .andExpect(content().string(containsString("TRUNG TÂM THÔNG BÁO")))
                .andExpect(content().string(containsString("Nguồn thông báo")));
    }

    @Test
    void rendersMemberWalletInEnglishWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        MemberAccount account = accounts.get(0);
        var memberUser = memberDetailsService.loadUserByUsername(account.getUsername());

        mockMvc.perform(get("/member/financial/transactions").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/wallet"))
                .andExpect(content().string(containsString("Wallet &amp; Fines")))
                .andExpect(content().string(containsString("Transaction History")));
    }

    @Test
    void rendersMemberFeesInEnglishWithCurrentDatabase() throws Exception {
        var accounts = memberAccountRepository.findAll(PageRequest.of(0, 1)).getContent();
        Assumptions.assumeFalse(accounts.isEmpty(), "Current database has no member account");
        MemberAccount account = accounts.get(0);
        var memberUser = memberDetailsService.loadUserByUsername(account.getUsername());

        mockMvc.perform(get("/member/financial/fees").with(user(memberUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("member/fees"))
                .andExpect(content().string(containsString("Borrowing Fees")))
                .andExpect(content().string(containsString("Loans Pending Approval")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianAcquisitionDashboardWithCurrentDatabase() throws Exception {
        mockMvc.perform(get("/librarian/dashboard").param("section", "acquisition"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/dashboard"));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianNotificationFormInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/interaction/notifications/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/send-notification"))
                .andExpect(content().string(containsString("Send Notifications to Members")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianAcquisitionListInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/interaction/acquisition-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/acquisition-request-list"))
                .andExpect(content().string(containsString("Book Acquisition Requests")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianTransactionsInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/members/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/transactions"))
                .andExpect(content().string(containsString("Transaction Management")))
                .andExpect(content().string(containsString("Clear Filter")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianReturnDeskInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/loan/returns"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/return-desk"))
                .andExpect(content().string(containsString("Book Return Desk")))
                .andExpect(content().string(containsString("Scan or enter the returned book barcode")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianProfileInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/profile"))
                .andExpect(content().string(containsString("Account Details")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianStorageFormInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/storage/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/storage-form"))
                .andExpect(content().string(containsString("Add New Location")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianBorrowListInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/borrow/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/borrow-list"))
                .andExpect(content().string(containsString("Loan and Return Management")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianReviewsInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/interaction/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/reviews-response"))
                .andExpect(content().string(containsString("Member Review Moderation")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianMemberListInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/members"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/member-list"))
                .andExpect(content().string(containsString("Create New Account")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianRefundsInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/members/refunds"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/refunds"))
                .andExpect(content().string(containsString("Refund Approval")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianBorrowScheduleInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/loan/borrow-schedule"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/borrow-schedule"))
                .andExpect(content().string(containsString("Detailed Loan Schedule")));
    }

    @Test
    @WithUserDetails(value = "librarian01", userDetailsServiceBeanName = "customStaffDetailsService")
    void rendersLibrarianSchedulePreviewInEnglish() throws Exception {
        mockMvc.perform(get("/librarian/loan/borrow-schedule/detail-preview"))
                .andExpect(status().isOk())
                .andExpect(view().name("librarian/borrow-schedule-detail"))
                .andExpect(content().string(containsString("Related Transactions")));
    }
}
