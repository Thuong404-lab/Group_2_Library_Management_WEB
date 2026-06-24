
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
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;


    public CustomOAuth2UserService(AccountRepository accountRepository, UserRepository userRepository, MemberRepository memberRepository, WalletRepository walletRepository, SystemLogRepository systemLogRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);  // Gọi super.loadUser để tránh bị StackOverflow
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Account account = accountRepository.findByUsername(email).orElse(null);
        if (account == null) {
            User user = new User();
            user.setFullName(name);
            user.setEmail(email);
            user.setPhone("");
            user.setStatus(UserStatus.Active);
            user = userRepository.save(user);

            account = new Account();
            account.setUsername(name);
            account.setPasswordHash("");
            account.setUser(user);
            account = accountRepository.save(account);

            Member member = new Member();
            member.setUser(user);
            member = memberRepository.save(member);

            Wallet wallet = new Wallet();
            wallet.setMember(member);
            wallet.setBalance(BigDecimal.ZERO);
            wallet = walletRepository.save(wallet);

        }
        return new CustomUserDetails(account, oAuth2User.getAuthorities(), oAuth2User.getAttributes());

    }

}



