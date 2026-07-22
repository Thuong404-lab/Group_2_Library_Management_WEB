package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.User;
import com.lms.exception.AccountFormValidationException;
import com.lms.repository.MemberAccountDeletionRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.UserRepository;
import com.lms.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    @Test
    void adminCannotDeleteMemberWithBusinessHistory() {
        Fixture fixture = fixture(true);

        assertThatThrownBy(() -> fixture.service.deleteAccount(7, "members", 1))
                .isInstanceOf(AccountFormValidationException.class);

        verify(fixture.deletionRepository, never()).deleteAggregate(7, 17, 27);
    }

    @Test
    void adminPhysicallyDeletesMemberWithoutBusinessHistory() {
        Fixture fixture = fixture(false);

        fixture.service.deleteAccount(7, "members", 1);

        verify(fixture.deletionRepository).deleteAggregate(7, 17, 27);
    }

    private Fixture fixture(boolean hasHistory) {
        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        MemberAccountDeletionRepository deletionRepository = mock(MemberAccountDeletionRepository.class);
        AuditLogService auditLogService = mock(AuditLogService.class);

        User user = new User();
        user.setId(27);
        Member member = new Member();
        member.setMemberId(17);
        member.setUser(user);
        MemberAccount account = new MemberAccount();
        account.setId(7);
        account.setUsername("member07");
        account.setMember(member);

        when(accountRepository.findById(7)).thenReturn(Optional.of(account));
        when(deletionRepository.hasBusinessHistory(17)).thenReturn(hasHistory);

        AccountServiceImpl service = new AccountServiceImpl(
                accountRepository,
                mock(StaffAccountRepository.class),
                mock(PasswordEncoder.class),
                mock(MemberRepository.class),
                mock(MembershipTierRepository.class),
                mock(UserRepository.class),
                mock(RoleRepository.class),
                mock(StaffRepository.class),
                auditLogService,
                deletionRepository);
        return new Fixture(service, deletionRepository);
    }

    private record Fixture(AccountServiceImpl service,
                           MemberAccountDeletionRepository deletionRepository) {
    }
}
