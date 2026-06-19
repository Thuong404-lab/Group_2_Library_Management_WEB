package com.lms.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

/**
 * CustomUserDetails - Đối tượng chứa thông tin User cho Spring Security
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
public class CustomUserDetails implements UserDetails {

    // TODO: Implement - Thêm các field: accountId, username, password, role
    // TODO: Implement tất cả các method của UserDetails interface

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO: Implement - Trả về ROLE_ADMIN / ROLE_LIBRARIAN / ROLE_MEMBER
        return null;
    }

    @Override
    public String getPassword() {
        // TODO: Implement
        return null;
    }

    @Override
    public String getUsername() {
        // TODO: Implement
        return null;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // TODO: Implement - Check account status == "Active"
        return true;
    }
}
