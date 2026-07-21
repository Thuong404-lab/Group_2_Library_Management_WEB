package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Member_Accounts")
public class MemberAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long version;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String status = "Active";

    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage = "en";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "Member_Account_Roles",
        joinColumns = @JoinColumn(name = "member_account_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    public MemberAccount() {
    }

    public MemberAccount(Integer id, Member member, String username, String passwordHash, String status) {
        this.id = id;
        this.member = member;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    /**
     * Backward-compatible property used by existing admin/librarian views.
     */
    @Transient
    public Integer getAccountId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    /**
     * Exposes the user associated through the member profile to legacy views.
     */
    @Transient
    public User getUser() {
        return member != null ? member.getUser() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public java.util.Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(java.util.Set<Role> roles) {
        this.roles = roles;
    }
}
