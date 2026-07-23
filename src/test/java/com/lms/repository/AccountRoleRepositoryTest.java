package com.lms.repository;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.Role;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:account-role-test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;NON_KEYWORDS=TRANSACTION,VALUE,CONDITION;INIT=CREATE SCHEMA IF NOT EXISTS dbo"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberAccountRepository memberAccountRepository;

    @Autowired
    private StaffAccountRepository staffAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void memberAccountLoadsCanonicalRoleFromJoinTable() {
        Role role = persistRole("ROLE_MEMBER");
        User user = persistUser("Member Test", "member-role@test.local");
        Member member = new Member();
        member.setUser(user);
        entityManager.persist(member);

        MemberAccount account = new MemberAccount();
        account.setMember(member);
        account.setUsername("member_role_test");
        account.setPasswordHash("encoded-password");
        account.setStatus("Active");
        account.getRoles().add(role);
        entityManager.persistAndFlush(account);
        entityManager.clear();

        MemberAccount reloaded = memberAccountRepository.findByUsername("member_role_test").orElseThrow();
        assertThat(reloaded.getRoles()).extracting(Role::getName).containsExactly("ROLE_MEMBER");
        assertThat(roleRepository.findByNameIgnoreCase("role_member")).isPresent();
    }

    @Test
    void staffAccountLoadsCanonicalRoleFromJoinTable() {
        Role role = persistRole("ROLE_LIBRARIAN");
        User user = persistUser("Librarian Test", "librarian-role@test.local");
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setStaffType("Librarian");
        entityManager.persist(staff);

        StaffAccount account = new StaffAccount();
        account.setStaff(staff);
        account.setUsername("librarian_role_test");
        account.setPasswordHash("encoded-password");
        account.setStatus("Active");
        account.getRoles().add(role);
        entityManager.persistAndFlush(account);
        entityManager.clear();

        StaffAccount reloaded = staffAccountRepository.findByUsername("librarian_role_test").orElseThrow();
        assertThat(reloaded.getRoles()).extracting(Role::getName).containsExactly("ROLE_LIBRARIAN");
    }

    @Test
    void memberAccountSearchTreatsNumericInputAsPhoneAndMemberId() {
        User user = persistUser("Numeric Search Member", "numeric-search@test.local");
        user.setPhone("0910000001");
        Member member = new Member();
        member.setUser(user);
        entityManager.persist(member);

        MemberAccount account = new MemberAccount();
        account.setMember(member);
        account.setUsername("numeric_search_member");
        account.setPasswordHash("encoded-password");
        account.setStatus("Blocked");
        entityManager.persistAndFlush(account);
        entityManager.clear();

        assertThat(memberAccountRepository.searchMemberAccounts("0910000001", PageRequest.of(0, 20)))
                .extracting(MemberAccount::getUsername)
                .containsExactly("numeric_search_member");
        assertThat(memberAccountRepository.searchMemberAccounts(
                member.getMemberId().toString(), PageRequest.of(0, 20)))
                .extracting(MemberAccount::getUsername)
                .contains("numeric_search_member");
    }

    private Role persistRole(String name) {
        Role role = new Role();
        role.setName(name);
        return entityManager.persist(role);
    }

    private User persistUser(String fullName, String email) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setStatus(UserStatus.Active);
        return entityManager.persist(user);
    }
}
