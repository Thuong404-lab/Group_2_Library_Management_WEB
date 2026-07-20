package com.lms.config;

import com.lms.entity.MemberAccount;
import com.lms.repository.MemberAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

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
        String langParam = normalizeLanguage(request.getParameter("lang"));

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
                    if (langParam != null) {
                        // User explicitly changed language via ?lang=
                        if (!langParam.equals(account.getPreferredLanguage())) {
                            account.setPreferredLanguage(langParam);
                            memberAccountRepository.save(account);
                        }
                    } else if (account.getPreferredLanguage() != null && !account.getPreferredLanguage().isBlank()) {
                        // Request has no ?lang= parameter -> enforce logged-in member's DB preferredLanguage
                        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
                        if (localeResolver != null) {
                            Locale currentLocale = localeResolver.resolveLocale(request);
                            if (!currentLocale.getLanguage().equals(account.getPreferredLanguage())) {
                                localeResolver.setLocale(request, response, Locale.forLanguageTag(account.getPreferredLanguage()));
                            }
                        }
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
