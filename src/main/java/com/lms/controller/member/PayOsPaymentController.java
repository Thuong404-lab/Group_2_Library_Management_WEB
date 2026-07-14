package com.lms.controller.member;

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
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/member/payments/payos")
public class PayOsPaymentController {
    private final PayOsPaymentService payOsPaymentService;
    private final MemberRepository memberRepository;

    public PayOsPaymentController(PayOsPaymentService payOsPaymentService,
                                  MemberRepository memberRepository) {
        this.payOsPaymentService = payOsPaymentService;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/top-up")
    public String createTopUp(@RequestParam BigDecimal amount, Principal principal,
                              RedirectAttributes redirectAttributes) {
        return createAndRedirect(() -> payOsPaymentService.createTopUp(currentMember(principal), amount),
                "/member/financial/transactions", redirectAttributes);
    }

    @PostMapping("/fine/{fineId}")
    public String createFinePayment(@PathVariable Integer fineId, Principal principal,
                                    RedirectAttributes redirectAttributes) {
        return createAndRedirect(() -> payOsPaymentService.createFinePayment(currentMember(principal), fineId),
                "/member/financial/transactions", redirectAttributes);
    }

    @PostMapping("/fine/all")
    public String createFineBatchPayment(Principal principal,
                                         RedirectAttributes redirectAttributes) {
        return createAndRedirect(() -> payOsPaymentService.createFineBatchPayment(currentMember(principal)),
                "/member/financial/transactions", redirectAttributes);
    }

    @PostMapping("/borrow-fee/{borrowId}")
    public String createBorrowFeePayment(@PathVariable Integer borrowId, Principal principal,
                                         RedirectAttributes redirectAttributes) {
        return createAndRedirect(() -> payOsPaymentService.createBorrowFeePayment(currentMember(principal), borrowId),
                "/member/financial/fees", redirectAttributes);
    }

    @GetMapping("/return")
    public String paymentReturn(@RequestParam(required = false) Long orderCode,
                                Principal principal, RedirectAttributes redirectAttributes) {
        if (orderCode == null) {
            redirectAttributes.addFlashAttribute("error", "Không xác định được đơn thanh toán KQPay.");
            return "redirect:/member/financial/transactions";
        }
        payOsPaymentService.getForMember(orderCode, currentMember(principal).getMemberId());
        return "redirect:/member/payments/payos/" + orderCode;
    }

    @GetMapping("/{orderCode}")
    public String viewPayment(@PathVariable Long orderCode, Principal principal, Model model) {
        PayOsPayment payment = payOsPaymentService.refreshForMember(orderCode, currentMember(principal).getMemberId());
        model.addAttribute("payment", payment);
        model.addAttribute("fineItems", payOsPaymentService.getFineItems(payment));
        model.addAttribute("paymentExpiresAt", payOsPaymentService.getExpiryEpochMillis(payment));
        return "member/payos-payment";
    }

    @GetMapping("/{orderCode}/status")
    @ResponseBody
    public Map<String, Object> paymentStatus(@PathVariable Long orderCode, Principal principal) {
        PayOsPayment payment = payOsPaymentService.refreshForMember(orderCode, currentMember(principal).getMemberId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderCode", payment.getOrderCode());
        result.put("status", payment.getStatus());
        result.put("paidAt", payment.getPaidAt());
        result.put("transactionId", payment.getTransaction() == null ? null : payment.getTransaction().getTransactionId());
        result.put("fineCount", payOsPaymentService.getFineItems(payment).size());
        return result;
    }

    @GetMapping(value = "/{orderCode}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qrImage(@PathVariable Long orderCode, Principal principal) throws Exception {
        PayOsPayment payment = payOsPaymentService.getForMember(orderCode, currentMember(principal).getMemberId());
        if (payment.getQrCode() == null || payment.getQrCode().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .body(PayOsQrImageRenderer.render(payment.getQrCode()));
    }

    private String createAndRedirect(PaymentCreator creator, String errorRedirect,
                                     RedirectAttributes redirectAttributes) {
        try {
            PayOsPayment payment = creator.create();
            return "redirect:/member/payments/payos/" + payment.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", readableMessage(e));
            return "redirect:" + errorRedirect;
        }
    }

    private Member currentMember(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Bạn cần đăng nhập để thanh toán.");
        }
        String login = principal.getName();
        return memberRepository.findByAccountUsername(login)
                .or(() -> memberRepository.findByUserEmail(login))
                .or(() -> memberRepository.findByUserPhone(login))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên hiện tại."));
    }

    private String readableMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Không thể tạo thanh toán KQPay." : current.getMessage();
    }

    @FunctionalInterface
    private interface PaymentCreator {
        PayOsPayment create();
    }
}
