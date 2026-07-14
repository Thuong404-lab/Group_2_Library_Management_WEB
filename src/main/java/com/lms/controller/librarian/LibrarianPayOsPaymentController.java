package com.lms.controller.librarian;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.repository.MemberRepository;
import com.lms.service.PayOsPaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/librarian/payments/payos")
public class LibrarianPayOsPaymentController {
    private final PayOsPaymentService paymentService;
    private final MemberRepository memberRepository;

    public LibrarianPayOsPaymentController(PayOsPaymentService paymentService,
                                           MemberRepository memberRepository) {
        this.paymentService = paymentService;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/top-up")
    public String createTopUp(@RequestParam String memberPhone,
                              @RequestParam BigDecimal amount,
                              RedirectAttributes redirectAttributes) {
        try {
            Member member = findMember(memberPhone);
            PayOsPayment payment = paymentService.createTopUpForLibrarian(member, amount);
            return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", readableMessage(e));
            redirectAttributes.addFlashAttribute("memberPhone", memberPhone);
            redirectAttributes.addFlashAttribute("amount", amount);
            return "redirect:/librarian/members/topup";
        }
    }

    @GetMapping("/return")
    public String paymentReturn(@RequestParam(required = false) Long orderCode,
                                RedirectAttributes redirectAttributes) {
        if (orderCode == null) {
            redirectAttributes.addFlashAttribute("error", "Không xác định được đơn thanh toán PayOS.");
            return "redirect:/librarian/members/topup";
        }
        paymentService.getForStaff(orderCode);
        return "redirect:/librarian/payments/payos/" + orderCode;
    }

    @GetMapping("/{orderCode}")
    public String viewPayment(@PathVariable Long orderCode, Model model) {
        model.addAttribute("payment", paymentService.refreshForStaff(orderCode));
        return "librarian/payos-topup";
    }

    @GetMapping("/{orderCode}/status")
    @ResponseBody
    public Map<String, Object> paymentStatus(@PathVariable Long orderCode) {
        PayOsPayment payment = paymentService.refreshForStaff(orderCode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderCode", payment.getOrderCode());
        result.put("status", payment.getStatus());
        result.put("paidAt", payment.getPaidAt());
        result.put("transactionId", payment.getTransaction() == null
                ? null : payment.getTransaction().getTransactionId());
        return result;
    }

    @GetMapping(value = "/{orderCode}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qrImage(@PathVariable Long orderCode) throws Exception {
        PayOsPayment payment = paymentService.getForStaff(orderCode);
        if (payment.getQrCode() == null || payment.getQrCode().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter().encode(
                payment.getQrCode(), BarcodeFormat.QR_CODE, 360, 360, hints);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(output.toByteArray());
    }

    private Member findMember(String lookup) {
        if (lookup == null || lookup.isBlank()) {
            throw new RuntimeException("Vui lòng chọn thành viên.");
        }
        String value = lookup.trim();
        if (value.matches("\\d+")) {
            try {
                return memberRepository.findById(Integer.valueOf(value))
                        .or(() -> memberRepository.findByUserPhone(value))
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên."));
            } catch (NumberFormatException ignored) {
                // Continue with the other lookup types below.
            }
        }
        return memberRepository.findByUserPhone(value)
                .or(() -> memberRepository.findByUserEmail(value))
                .or(() -> memberRepository.findByAccountUsername(value))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên."));
    }

    private String readableMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Không thể tạo mã QR PayOS." : current.getMessage();
    }
}
