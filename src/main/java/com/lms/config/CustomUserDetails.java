package com.lms.config;

import com.lms.entity.User;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {

    private final User user;
    private final String username;
    private final String passwordHash;
    private final String status;
    private final Integer accountId; // Can be MemberAccount.id or StaffAccount.id
    
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public CustomUserDetails(User user, String username, String passwordHash, String status, Integer accountId, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.accountId = accountId;
        this.authorities = authorities;
    }

    public CustomUserDetails(User user, String username, String passwordHash, String status, Integer accountId, Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        this.user = user;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.accountId = accountId;
        this.authorities = authorities;
        this.attributes = attributes;
    }

    public User getUser() {
        return user;
    }
    
    public Integer getAccountId() {
        return accountId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return "Active".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Nullable
    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean isEnabled() {
        return "Active".equalsIgnoreCase(status);
    }

    @Override
    public String getName() {
        return username;
    }
}
