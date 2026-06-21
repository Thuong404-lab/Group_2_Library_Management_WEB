/* 
package com.lms.service;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        Optional<Account> accountOpt = accountRepository.findByUsername(email);
        Account account;
        
        if (accountOpt.isPresent()) {
            account = accountOpt.get();
        } else {
            User user = new User();
            user.setFullName(name);
            user.setEmail(email);
            user.setStatus("Active");
            user = userRepository.save(user);

            account = new Account();
            account.setUsername(email);
            account.setPasswordHash("");
            account.setUser(user);
            account.setStatus("Active");
            accountRepository.save(account);

            Member member = new Member();
            member.setUser(user);
            memberRepository.save(member);

            Wallet wallet = new Wallet();
            wallet.setMember(member);
            wallet.setBalance(BigDecimal.ZERO);
            walletRepository.save(wallet);
        }

        return new CustomUserDetails(account, Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }
}
*/
