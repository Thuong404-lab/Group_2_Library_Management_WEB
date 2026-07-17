package com.lms.controller.member;

import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.repository.MemberRepository;
import com.lms.service.PayOsPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayOsPaymentControllerTest {

    @Test
    void rendersPayOsVietQrAsPngForThePaymentOwner() throws Exception {
        PayOsPaymentService paymentService = mock(PayOsPaymentService.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        PayOsPaymentController controller = new PayOsPaymentController(paymentService, memberRepository);

        Member member = new Member();
        member.setMemberId(7);
        PayOsPayment payment = new PayOsPayment();
        payment.setOrderCode(123456789L);
        payment.setQrCode("00020101021238570010A0000007270127000697042201131234567890208LMSPAYOS530370454061000005802VN6304ABCD");
        Principal principal = () -> "member7";

        when(memberRepository.findByAccountUsername("member7")).thenReturn(Optional.of(member));
        when(paymentService.getForMember(123456789L, 7)).thenReturn(payment);

        ResponseEntity<byte[]> response = controller.qrImage(123456789L, principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/png");
        assertThat(response.getBody()).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getBody()));
        boolean containsSystemBrown = false;
        for (int x = 0; x < image.getWidth() && !containsSystemBrown; x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == 0x4A3B32) {
                    containsSystemBrown = true;
                    break;
                }
            }
        }
        assertThat(containsSystemBrown).isTrue();
    }
}
