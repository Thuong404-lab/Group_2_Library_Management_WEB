package com.lms.service;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.exception.ValidationException;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.impl.MemberBookAcquisitionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MemberBookAcquisitionValidationServiceTest {

    private final MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
    private final BookAcquisitionRequestRepository requestRepository = mock(BookAcquisitionRequestRepository.class);
    private final MemberBookAcquisitionServiceImpl service =
            new MemberBookAcquisitionServiceImpl(accountRepository, requestRepository);

    @BeforeEach
    void useVietnameseLocale() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void rejectsBookTitleContainingOnlyNumbersAndSpecialCharacters() {
        MemberBookAcquisitionRequest request = validRequest();
        request.setTitle("12345!!!");

        assertThatThrownBy(() -> service.submitRequest("member", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("không được chỉ gồm số hoặc ký tự đặc biệt");
        verifyNoInteractions(accountRepository, requestRepository);
    }

    @Test
    void rejectsReasonShorterThanTenCharacters() {
        MemberBookAcquisitionRequest request = validRequest();
        request.setRequestReason("Quá ngắn");

        assertThatThrownBy(() -> service.submitRequest("member", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("10 đến 1.000 ký tự");
        verifyNoInteractions(accountRepository, requestRepository);
    }

    @Test
    void rejectsInvalidReferenceUrl() {
        MemberBookAcquisitionRequest request = validRequest();
        request.setReferenceUrl("https://");

        assertThatThrownBy(() -> service.submitRequest("member", request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("địa chỉ HTTP hoặc HTTPS hợp lệ");
        verifyNoInteractions(accountRepository, requestRepository);
    }

    private MemberBookAcquisitionRequest validRequest() {
        MemberBookAcquisitionRequest request = new MemberBookAcquisitionRequest();
        request.setTitle("Nhà Giả Kim");
        request.setAuthor("Paulo Coelho");
        request.setRequestReason("Tôi muốn thư viện bổ sung cuốn sách này.");
        return request;
    }
}
