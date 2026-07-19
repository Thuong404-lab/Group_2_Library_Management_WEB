package com.lms.config;

import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

@Component
public class MemberLocalePreferenceInterceptor implements HandlerInterceptor {

    private final MemberAccountRepository memberAccountRepository;

    public MemberLocalePreferenceInterceptor(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String language = normalizeLanguage(request.getParameter("lang"));
        if (language == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_MEMBER".equals(authority.getAuthority()))) {
            return true;
        }

        String usernameOrContact = authentication.getName();
        memberAccountRepository.findByUsername(usernameOrContact)
                .or(() -> memberAccountRepository.findByMember_User_Email(usernameOrContact))
                .or(() -> memberAccountRepository.findByMember_User_Phone(usernameOrContact))
                .ifPresent(account -> {
            if (!language.equals(account.getPreferredLanguage())) {
                account.setPreferredLanguage(language);
                memberAccountRepository.save(account);
            }
        });
        return true;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        String normalized = Locale.forLanguageTag(language).getLanguage();
        return "vi".equals(normalized) || "en".equals(normalized) ? normalized : null;
    }
}
