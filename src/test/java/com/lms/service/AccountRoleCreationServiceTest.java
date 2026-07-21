package com.lms.service;

import com.lms.dto.request.AdminAccountCreateRequest;
import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.entity.MemberAccount;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.repository.MemberAccountRepository;
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
        MembershipTier tier = new MembershipTier();
        Role memberRole = new Role(3, "ROLE_MEMBER");
        CreateMemberAccountRequest request = memberRequest();

        when(tierRepository.existsById(1)).thenReturn(true);
        when(tierRepository.findById(1)).thenReturn(Optional.of(tier));
        when(roleRepository.findByNameIgnoreCase("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Demo@123")).thenReturn("encoded-password");

        LibrarianMemberServiceImpl service = new LibrarianMemberServiceImpl(
                accountRepository,
                memberRepository,
                tierRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                mock(AuditLogService.class));

        service.createMember(request);

        ArgumentCaptor<MemberAccount> account = ArgumentCaptor.forClass(MemberAccount.class);
        verify(accountRepository).save(account.capture());
        assertThat(account.getValue().getRoles()).containsExactly(memberRole);
    }

    @Test
    void adminCreationUsesRequestedTierAndCanonicalRole() {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        MembershipTier requestedTier = new MembershipTier();
        Role memberRole = new Role(3, "ROLE_MEMBER");
        AdminAccountCreateRequest request = new AdminAccountCreateRequest(
                "Admin Created", "admin.created@test.local", "0900000001",
                "admin_created", "Demo@123", "MEMBER", 2, "Active");

        when(tierRepository.existsById(2)).thenReturn(true);
        when(tierRepository.findById(2)).thenReturn(Optional.of(requestedTier));
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

        service.createAccount(request);

        ArgumentCaptor<MemberAccount> account = ArgumentCaptor.forClass(MemberAccount.class);
        verify(accountRepository).save(account.capture());
        verify(roleRepository).findByNameIgnoreCase("ROLE_MEMBER");
        assertThat(account.getValue().getMember().getTier()).isSameAs(requestedTier);
        assertThat(account.getValue().getRoles()).containsExactly(memberRole);
    }

    private CreateMemberAccountRequest memberRequest() {
        CreateMemberAccountRequest request = new CreateMemberAccountRequest();
        request.setFullName("Member Created");
        request.setEmail("member.created@test.local");
        request.setPhone("0900000002");
        request.setUsername("member_created");
        request.setPassword("Demo@123");
        request.setTierId(1);
        request.setStatus("Active");
        return request;
    }
}
