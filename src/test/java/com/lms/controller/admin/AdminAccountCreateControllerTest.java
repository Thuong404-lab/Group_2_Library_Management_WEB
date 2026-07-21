package com.lms.controller.admin;

import com.lms.dto.request.AdminMemberAccountCreateRequest;
import com.lms.dto.request.AdminStaffAccountCreateRequest;
import com.lms.service.AccountService;
import com.lms.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAccountCreateControllerTest {

    private AccountService accountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        AccountController memberController = new AccountController(accountService, mock(AuthService.class));
        StaffAccountController staffController = new StaffAccountController(accountService);
        mockMvc = MockMvcBuilders.standaloneSetup(memberController, staffController).build();
    }

    @Test
    void memberValidationEndpointBuildsMemberOnlyRequest() throws Exception {
        when(accountService.validateMemberAccountCreate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of());

        mockMvc.perform(get("/admin/accounts/create/validate")
                        .param("fullName", "Member One")
                        .param("email", "member.one@test.local")
                        .param("phone", "0900000001")
                        .param("username", "member_one")
                        .param("password", "Demo@123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(accountService).validateMemberAccountCreate(argThat(request ->
                request instanceof AdminMemberAccountCreateRequest
                        && "member_one".equals(request.getUsername())));
    }

    @Test
    void staffValidationEndpointBuildsStaffOnlyRequest() throws Exception {
        when(accountService.validateStaffAccountCreate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of());

        mockMvc.perform(get("/admin/staff/create/validate")
                        .param("fullName", "Staff One")
                        .param("email", "staff.one@test.local")
                        .param("phone", "0900000002")
                        .param("username", "staff_one")
                        .param("password", "Demo@123")
                        .param("staffType", "Librarian")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(accountService).validateStaffAccountCreate(argThat(request ->
                request instanceof AdminStaffAccountCreateRequest
                        && "Librarian".equals(request.getStaffType())));
    }

    @Test
    void memberCreateEndpointUsesMemberFlowAndReturnsToMemberList() throws Exception {
        mockMvc.perform(post("/admin/accounts/create")
                        .param("fullName", "Member One")
                        .param("email", "member.one@test.local")
                        .param("phone", "0900000001")
                        .param("username", "member_one")
                        .param("password", "Demo@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"));

        verify(accountService).createMemberAccount(argThat(request ->
                "member_one".equals(request.getUsername())));
    }

    @Test
    void staffCreateEndpointUsesStaffFlowAndReturnsToStaffList() throws Exception {
        mockMvc.perform(post("/admin/staff/create")
                        .param("fullName", "Staff One")
                        .param("email", "staff.one@test.local")
                        .param("phone", "0900000002")
                        .param("username", "staff_one")
                        .param("password", "Demo@123")
                        .param("staffType", "Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/staff"));

        verify(accountService).createStaffAccount(argThat(request ->
                "Admin".equals(request.getStaffType())));
    }
}
