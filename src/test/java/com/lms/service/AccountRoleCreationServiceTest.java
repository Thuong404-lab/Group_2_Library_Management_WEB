package com.lms.service;

import com.lms.dto.request.AdminMemberAccountCreateRequest;
import com.lms.dto.request.AdminStaffAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberAccountDeletionRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.PasswordResetTokenRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.impl.AccountServiceImpl;
import com.lms.service.impl.AuthServiceImpl;
import com.lms.service.impl.LibrarianMemberServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountRoleCreationServiceTest {

    @Test
    void coreOAuthAccountGeneratesPasswordHashAndAssignsCanonicalMemberRole() {
        UserRepository userRepository = mock(UserRepository.class);
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Role memberRole = new Role(3, "ROLE_MEMBER");
        String generatedPasswordHash = "$2a$10$0ZPpDwDviBhxvqCU0rl46uMDpgIrZ93eBGJXDmJMXqlYmUTBuoTlW";

        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByNameIgnoreCase("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode(any())).thenReturn(generatedPasswordHash);

        AuthServiceImpl service = new AuthServiceImpl(
                userRepository,
                accountRepository,
                mock(StaffAccountRepository.class),
                memberRepository,
                walletRepository,
                roleRepository,
                tierRepository,
                passwordEncoder,
                mock(PasswordResetTokenRepository.class),
                mock(EmailService.class),
                mock(SystemLogRepository.class),
                "http://localhost:8080");

        MemberAccount account = service.createCoreAccount(
                "new_member", "New Member", "", "new.member@test.local", "0900000000");

        assertThat(account.getRoles()).containsExactly(memberRole);
        assertThat(account.getPasswordHash()).isEqualTo(generatedPasswordHash);
        verify(passwordEncoder).encode(any());
        verify(walletRepository).save(any());
    }

    @Test
    void librarianCreationAssignsCanonicalMemberRole() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        MembershipTier tier = new MembershipTier();
        Role memberRole = new Role(3, "ROLE_MEMBER");
        CreateMemberAccountRequest request = memberRequest();

        when(tierRepository.findFirstByOrderByConditionAscTierIdAsc()).thenReturn(Optional.of(tier));
        when(roleRepository.findByNameIgnoreCase("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Demo@123")).thenReturn("encoded-password");

        LibrarianMemberServiceImpl service = new LibrarianMemberServiceImpl(
                accountRepository,
                memberRepository,
                tierRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                mock(AuditLogService.class),
                walletRepository,
                mock(MemberAccountDeletionRepository.class));

        service.createMember(request);

        ArgumentCaptor<MemberAccount> account = ArgumentCaptor.forClass(MemberAccount.class);
        verify(accountRepository).saveAndFlush(account.capture());
        assertThat(account.getValue().getRoles()).containsExactly(memberRole);
        verify(walletRepository).save(any());
    }

    @Test
    void adminMemberCreationUsesServerControlledRegularTierAndCanonicalRole() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MembershipTier regularTier = new MembershipTier();
        Role memberRole = new Role(3, "ROLE_MEMBER");
        AdminMemberAccountCreateRequest request = new AdminMemberAccountCreateRequest(
                "Admin Created", "admin.created@test.local", "0900000001",
                "admin_created", "Demo@123");

        when(tierRepository.findFirstByOrderByConditionAscTierIdAsc()).thenReturn(Optional.of(regularTier));
        when(roleRepository.findByNameIgnoreCase("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Demo@123")).thenReturn("encoded-password");

        AccountServiceImpl service = new AccountServiceImpl(
                accountRepository,
                staffAccountRepository,
                passwordEncoder,
                memberRepository,
                tierRepository,
                userRepository,
                roleRepository,
                mock(StaffRepository.class),
                mock(AuditLogService.class));

        service.createMemberAccount(request);

        ArgumentCaptor<MemberAccount> account = ArgumentCaptor.forClass(MemberAccount.class);
        verify(accountRepository).save(account.capture());
        verify(roleRepository).findByNameIgnoreCase("ROLE_MEMBER");
        assertThat(account.getValue().getMember().getTier()).isSameAs(regularTier);
        assertThat(account.getValue().getStatus()).isEqualTo("Active");
        assertThat(account.getValue().getMember().getUser().getStatus().name()).isEqualTo("Active");
        assertThat(account.getValue().getRoles()).containsExactly(memberRole);
    }

    @Test
    void adminStaffCreationUsesDedicatedStaffTypeAndCanonicalRole() {
        MemberAccountRepository memberAccountRepository = mock(MemberAccountRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Role librarianRole = new Role(2, "ROLE_LIBRARIAN");
        AdminStaffAccountCreateRequest request = new AdminStaffAccountCreateRequest(
                "New Librarian", "new.librarian@test.local", "0900000002",
                "new_librarian", "Demo@123", "Librarian");

        when(roleRepository.findByNameIgnoreCase("ROLE_LIBRARIAN")).thenReturn(Optional.of(librarianRole));
        when(passwordEncoder.encode("Demo@123")).thenReturn("encoded-password");

        AccountServiceImpl service = new AccountServiceImpl(
                memberAccountRepository,
                staffAccountRepository,
                passwordEncoder,
                mock(MemberRepository.class),
                tierRepository,
                userRepository,
                roleRepository,
                staffRepository,
                mock(AuditLogService.class));

        service.createStaffAccount(request);

        ArgumentCaptor<StaffAccount> account = ArgumentCaptor.forClass(StaffAccount.class);
        verify(staffAccountRepository).save(account.capture());
        assertThat(account.getValue().getStaff().getStaffType()).isEqualTo("Librarian");
        assertThat(account.getValue().getStatus()).isEqualTo("Active");
        assertThat(account.getValue().getStaff().getUser().getStatus().name()).isEqualTo("Active");
        assertThat(account.getValue().getRoles()).containsExactly(librarianRole);
    }

    @Test
    void memberCreationValidationReportsMissingDefaultTierAsGlobalError() {
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        AdminMemberAccountCreateRequest request = new AdminMemberAccountCreateRequest(
                "Valid Member", "valid.member@test.local", "0900000001",
                "valid_member", "Demo@123");
        AccountServiceImpl service = accountService(
                mock(MemberAccountRepository.class), mock(StaffAccountRepository.class),
                mock(UserRepository.class), tierRepository);

        assertThat(service.validateMemberAccountCreate(request))
                .containsKey("_global")
                .doesNotContainKey("tierId");
    }

    @Test
    void staffCreationValidationRejectsInvalidStaffTypeWithoutMembershipTierValidation() {
        AdminStaffAccountCreateRequest request = new AdminStaffAccountCreateRequest(
                "Valid Staff", "valid.staff@test.local", "0900000001",
                "valid_staff", "Demo@123", "MEMBER");
        AccountServiceImpl service = accountService(
                mock(MemberAccountRepository.class), mock(StaffAccountRepository.class),
                mock(UserRepository.class), mock(MembershipTierRepository.class));

        assertThat(service.validateStaffAccountCreate(request))
                .containsKey("staffType")
                .doesNotContainKeys("tierId", "_global");
    }

    @Test
    void memberUpdateValidationDoesNotRequireReadOnlyTierField() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setId(21);
        Member member = new Member();
        member.setUser(user);
        MemberAccount account = new MemberAccount();
        account.setId(12);
        account.setMember(member);
        AdminAccountUpdateRequest request = new AdminAccountUpdateRequest(
                12, "Valid Member", "valid.member@test.local", "0900000001",
                "valid_member", null, null, "Active", "members");

        when(accountRepository.findById(12)).thenReturn(Optional.of(account));

        AccountServiceImpl service = accountService(
                accountRepository, staffAccountRepository, userRepository, mock(MembershipTierRepository.class));

        assertThat(service.validateAccountUpdate(request, 1)).isEmpty();
    }

    @Test
    void accountValidationRejectsEmailLongerThanDatabaseColumn() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        String oversizedEmail = "a".repeat(244) + "@example.com";
        AdminMemberAccountCreateRequest request = new AdminMemberAccountCreateRequest(
                "Valid Member", oversizedEmail, "0900000001",
                "valid_member", "Demo@123");

        when(tierRepository.findFirstByOrderByConditionAscTierIdAsc()).thenReturn(Optional.of(new MembershipTier()));

        AccountServiceImpl service = accountService(
                accountRepository, staffAccountRepository, userRepository, tierRepository);

        assertThat(service.validateMemberAccountCreate(request))
                .containsEntry("email", "Email cannot exceed 255 characters.");
    }

    @Test
    void accountValidationReportsRequiredAndDuplicateFieldsTogether() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        AdminMemberAccountCreateRequest request = new AdminMemberAccountCreateRequest(
                "", "existing.member@test.local", "0900000001",
                "valid_member", "Demo@123");

        when(userRepository.existsByEmail("existing.member@test.local")).thenReturn(true);
        when(tierRepository.findFirstByOrderByConditionAscTierIdAsc()).thenReturn(Optional.of(new MembershipTier()));

        AccountServiceImpl service = accountService(
                accountRepository, staffAccountRepository, userRepository, tierRepository);

        assertThat(service.validateMemberAccountCreate(request))
                .containsKeys("fullName", "email");
    }

    private AccountServiceImpl accountService(MemberAccountRepository accountRepository,
            StaffAccountRepository staffAccountRepository,
            UserRepository userRepository,
            MembershipTierRepository tierRepository) {
        return new AccountServiceImpl(
                accountRepository,
                staffAccountRepository,
                mock(PasswordEncoder.class),
                mock(MemberRepository.class),
                tierRepository,
                userRepository,
                mock(RoleRepository.class),
                mock(StaffRepository.class),
                mock(AuditLogService.class));
    }

    private CreateMemberAccountRequest memberRequest() {
        CreateMemberAccountRequest request = new CreateMemberAccountRequest();
        request.setFullName("Member Created");
        request.setEmail("member.created@test.local");
        request.setPhone("0900000002");
        request.setUsername("member_created");
        request.setPassword("Demo@123");
        request.setConfirmPassword("Demo@123");
        return request;
    }
}
