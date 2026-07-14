package com.lms.controller.librarian;

import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.repository.MemberRepository;
import com.lms.service.PayOsPaymentService;
import com.lms.util.PayOsQrImageRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
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
            redirectAttributes.addFlashAttribute("error", "Không xác định được đơn thanh toán KQPay.");
            return "redirect:/librarian/members/topup";
        }
        paymentService.getForStaff(orderCode);
        return "redirect:/librarian/payments/payos/" + orderCode;
    }

    @GetMapping("/{orderCode}")
    public String viewPayment(@PathVariable Long orderCode, Model model) {
        PayOsPayment payment = paymentService.refreshForStaff(orderCode);
        model.addAttribute("payment", payment);
        model.addAttribute("paymentExpiresAt", paymentService.getExpiryEpochMillis(payment));
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
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .body(PayOsQrImageRenderer.render(payment.getQrCode()));
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
        return current.getMessage() == null ? "Không thể tạo mã QR KQPay." : current.getMessage();
    }
}
