package com.lms.service.impl;

import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountDeletionRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibrarianMemberServiceImplTest {

    private final MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MembershipTierRepository tierRepository = mock(MembershipTierRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final MemberAccountDeletionRepository deletionRepository = mock(MemberAccountDeletionRepository.class);

    private LibrarianMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LibrarianMemberServiceImpl(
                accountRepository,
                memberRepository,
                tierRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                auditLogService,
                walletRepository,
                deletionRepository);
    }

    @Test
    void createBuildsCompleteActiveMemberAggregateFromDatabaseDefaultTier() {
        MembershipTier defaultTier = new MembershipTier();
        defaultTier.setTierId(7);
        defaultTier.setCondition(BigDecimal.ZERO);
        Role memberRole = new Role(3, "ROLE_MEMBER");
        CreateMemberAccountRequest request = validCreateRequest();
        when(tierRepository.findFirstByOrderByConditionAscTierIdAsc()).thenReturn(Optional.of(defaultTier));
        when(roleRepository.findByNameIgnoreCase("ROLE_MEMBER")).thenReturn(Optional.of(memberRole));
        when(passwordEncoder.encode("Demo@123")).thenReturn("encoded");

        service.createMember(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        ArgumentCaptor<MemberAccount> accountCaptor = ArgumentCaptor.forClass(MemberAccount.class);
        verify(memberRepository).save(memberCaptor.capture());
        verify(walletRepository).save(walletCaptor.capture());
        verify(accountRepository).saveAndFlush(accountCaptor.capture());

        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getTier()).isSameAs(defaultTier);
        assertThat(savedMember.getUser().getStatus()).isEqualTo(UserStatus.Active);
        assertThat(walletCaptor.getValue().getMember()).isSameAs(savedMember);
        assertThat(walletCaptor.getValue().getBalance()).isZero();
        assertThat(accountCaptor.getValue().getStatus()).isEqualTo("Active");
        assertThat(accountCaptor.getValue().getRoles()).containsExactly(memberRole);
    }

    @Test
    void createValidationRejectsMismatchedPasswordConfirmation() {
        CreateMemberAccountRequest request = validCreateRequest();
        request.setConfirmPassword("Different@123");

        assertThat(service.validateCreate(request))
                .containsKey("confirmPassword");
    }

    @Test
    void deleteRejectsAccountThatHasBusinessHistory() {
        MemberAccount account = account(11, 21, 31, "member11");
        when(accountRepository.findById(11)).thenReturn(Optional.of(account));
        when(deletionRepository.hasBusinessHistory(21)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteMember(11))
                .isInstanceOf(ConflictException.class);
        verify(deletionRepository, never()).deleteAggregate(any(), any(), any());
    }

    @Test
    void deleteRemovesUnusedAggregateAndWritesAudit() {
        MemberAccount account = account(12, 22, 32, "member12");
        when(accountRepository.findById(12)).thenReturn(Optional.of(account));
        when(deletionRepository.hasBusinessHistory(22)).thenReturn(false);

        service.deleteMember(12);

        verify(deletionRepository).deleteAggregate(12, 22, 32);
        verify(auditLogService).log(eq(ActionType.DELETE_ACCOUNT), anyString());
    }

    @Test
    void statusChangeRejectsUnknownValueInsteadOfFallingBackToActive() {
        assertThatThrownBy(() -> service.changeMemberStatus(1, "UNKNOWN"))
                .isInstanceOf(ValidationException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void statusChangeSynchronizesUserAndWritesAudit() {
        MemberAccount account = account(13, 23, 33, "member13");
        account.setStatus("Active");
        account.getMember().getUser().setStatus(UserStatus.Active);
        when(accountRepository.findById(13)).thenReturn(Optional.of(account));

        service.changeMemberStatus(13, "Inactive");

        assertThat(account.getStatus()).isEqualTo("Inactive");
        assertThat(account.getMember().getUser().getStatus()).isEqualTo(UserStatus.Inactive);
        verify(auditLogService).log(eq(ActionType.UPDATE_ACCOUNT), anyString());
    }

    private CreateMemberAccountRequest validCreateRequest() {
        CreateMemberAccountRequest request = new CreateMemberAccountRequest();
        request.setFullName("Nguyễn Văn An");
        request.setEmail("member@test.local");
        request.setPhone("0912345678");
        request.setUsername("member_new");
        request.setPassword("Demo@123");
        request.setConfirmPassword("Demo@123");
        return request;
    }

    private MemberAccount account(int accountId, int memberId, int userId, String username) {
        User user = new User();
        user.setId(userId);
        Member member = new Member();
        member.setMemberId(memberId);
        member.setUser(user);
        MemberAccount account = new MemberAccount();
        account.setId(accountId);
        account.setMember(member);
        account.setUsername(username);
        return account;
    }
}
