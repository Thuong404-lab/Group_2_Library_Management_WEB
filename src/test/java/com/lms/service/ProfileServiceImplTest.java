package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.exception.ValidationException;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.UserRepository;
import com.lms.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock MemberAccountRepository memberAccountRepository;
    @Mock StaffAccountRepository staffAccountRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FileUploadService fileUploadService;
    @Mock BorrowDetailRepository borrowDetailRepository;
    @Mock LocalizedMessageService messages;

    @InjectMocks ProfileServiceImpl service;

    @Test
    void countActiveBorrowsUsesMemberResolvedFromAuthenticatedUsername() {
        Member member = new Member();
        member.setMemberId(12);
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        when(memberAccountRepository.findByUsername("member01")).thenReturn(Optional.of(account));
        when(borrowDetailRepository.countActiveBorrowedBooks(12)).thenReturn(3L);

        assertThat(service.countActiveBorrows("member01")).isEqualTo(3L);
        verify(borrowDetailRepository).countActiveBorrowedBooks(12);
    }

    @Test
    void updateProfileRejectsNonImageAvatarBeforeUpload() {
        User user = new User();
        user.setId(8);
        user.setFullName("Member Name");
        user.setEmail("member@example.com");
        user.setPhone("0900000001");
        Member member = new Member();
        member.setUser(user);
        MemberAccount account = new MemberAccount();
        account.setMember(member);
        account.setUsername("member01");
        when(memberAccountRepository.findByUsername("member01")).thenReturn(Optional.of(account));
        when(messages.get("validation.avatarType")).thenReturn("Invalid image type");
        MockMultipartFile invalidAvatar = new MockMultipartFile(
                "avatarFile", "avatar.txt", "text/plain", "not an image".getBytes());

        assertThatThrownBy(() -> service.updateProfile(
                "member01", "member01", "Member Name", "member@example.com",
                "0900000001", invalidAvatar))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid image type");

        verify(fileUploadService, never()).storeFile(invalidAvatar);
        verify(userRepository, never()).save(user);
    }

    @Test
    void staffProfileNeverFallsBackToMemberWithTheSameUsername() {
        User staffUser = new User();
        staffUser.setFullName("Staff User");
        Staff staff = new Staff(2, staffUser, "Librarian");
        StaffAccount staffAccount = new StaffAccount(4, staff, "shared", "hash", "Active");
        when(staffAccountRepository.findByUsername("shared")).thenReturn(Optional.of(staffAccount));

        assertThat(service.getStaffProfile("shared")).isSameAs(staffUser);
        verifyNoInteractions(memberAccountRepository);
    }

    @Test
    void changeStaffPasswordNeverUpdatesMemberWithTheSameUsername() {
        StaffAccount staffAccount = new StaffAccount();
        staffAccount.setUsername("shared");
        staffAccount.setPasswordHash("old-hash");
        when(staffAccountRepository.findByUsername("shared")).thenReturn(Optional.of(staffAccount));
        when(passwordEncoder.matches("Current123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");

        service.changeStaffPassword("shared", "Current123", "NewPassword123");

        assertThat(staffAccount.getPasswordHash()).isEqualTo("new-hash");
        verify(staffAccountRepository).save(staffAccount);
        verifyNoInteractions(memberAccountRepository);
    }
}
