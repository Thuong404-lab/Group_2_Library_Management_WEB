package com.lms.service;

import com.lms.config.CustomUserDetails;
import com.lms.entity.*;
import com.lms.repository.*;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberAccountRepository memberAccountRepository;
    private final AuthService authService;
    private final LocalizedMessageService messages;

    public CustomOAuth2UserService(MemberAccountRepository memberAccountRepository,
                                   AuthService authService,
                                   LocalizedMessageService messages) {
        this.memberAccountRepository = memberAccountRepository;
        this.authService = authService;
        this.messages = messages;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        MemberAccount account = memberAccountRepository.findByMember_User_Email(email).orElse(null);
        if (account == null) {
            String baseUsername = email.substring(0, email.indexOf("@"));
            String generatedUsername = baseUsername;
            int suffix = 1;

            while (memberAccountRepository.existsByUsername(generatedUsername)) {
                generatedUsername = baseUsername + suffix;
                suffix++;
            }

            account = authService.createCoreAccount(generatedUsername, name, "", email, "");
        }
        if ("Blocked".equalsIgnoreCase(account.getStatus())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_blocked"),
                    messages.get("auth.accountLocked"));
        }
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>(
                oAuth2User.getAuthorities());
        account.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                .forEach(authorities::add);

        return new CustomUserDetails(
                account.getMember().getUser(),
                account.getUsername(),
                account.getPasswordHash(),
                account.getStatus(),
                account.getId(),
                authorities,
                oAuth2User.getAttributes());
    }
}
