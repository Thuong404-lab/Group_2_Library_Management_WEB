package com.lms.config;

import com.lms.service.CustomOAuth2UserService;
import com.lms.service.impl.CustomMemberDetailsService;
import com.lms.service.impl.CustomStaffDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecurityConfigAuthenticationProviderTest {

    @Test
    void staffProviderUsesOnlyStaffDetailsService() {
        CustomStaffDetailsService staffDetails = mock(CustomStaffDetailsService.class);
        CustomMemberDetailsService memberDetails = mock(CustomMemberDetailsService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(staffDetails.loadUserByUsername("admin")).thenReturn(
                User.withUsername("admin").password("hash").roles("ADMIN").build());
        when(encoder.matches("password", "hash")).thenReturn(true);
        SecurityConfig config = new SecurityConfig(mock(CustomOAuth2UserService.class));
        AuthenticationProvider provider = config.staffAuthenticationProvider(staffDetails, encoder);

        assertThat(provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("admin", "password")).isAuthenticated()).isTrue();

        verify(staffDetails).loadUserByUsername("admin");
        verifyNoInteractions(memberDetails);
    }

    @Test
    void memberProviderUsesOnlyMemberDetailsService() {
        CustomStaffDetailsService staffDetails = mock(CustomStaffDetailsService.class);
        CustomMemberDetailsService memberDetails = mock(CustomMemberDetailsService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(memberDetails.loadUserByUsername("member01")).thenReturn(
                User.withUsername("member01").password("hash").roles("MEMBER").build());
        when(encoder.matches("password", "hash")).thenReturn(true);
        SecurityConfig config = new SecurityConfig(mock(CustomOAuth2UserService.class));
        AuthenticationProvider provider = config.memberAuthenticationProvider(memberDetails, encoder);

        assertThat(provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("member01", "password")).isAuthenticated()).isTrue();

        verify(memberDetails).loadUserByUsername("member01");
        verifyNoInteractions(staffDetails);
    }
}
