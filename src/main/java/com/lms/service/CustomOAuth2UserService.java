
package com.lms.service;

import com.lms.config.CustomUserDetails;
import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
import com.lms.repository.*;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AccountRepository accountRepository;
    private final AuthService authService;


    public CustomOAuth2UserService(AccountRepository accountRepository, UserRepository userRepository, MemberRepository memberRepository, WalletRepository walletRepository, SystemLogRepository systemLogRepository, AuthService authService) {
        this.accountRepository = accountRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);  // Gọi super.loadUser để tránh bị StackOverflow
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Account account = accountRepository.findByUser_Email(email).orElse(null);
        if (account == null) {
            //  String userName, String fullName, String pass, String email, String phone);
            account = authService.createCoreAccount(email, name, "", email, "");

        }
        return new CustomUserDetails(account, oAuth2User.getAuthorities(), oAuth2User.getAttributes());

    }

}



