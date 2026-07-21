package com.lms.controller.librarian;

import com.lms.exception.ApplicationException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.entity.Staff;
import com.lms.config.CustomUserDetails;
import com.lms.repository.MemberRepository;
import com.lms.repository.StaffRepository;
import com.lms.service.PayOsPaymentService;
import com.lms.util.PayOsQrImageRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/librarian/payments/payos")
public class LibrarianPayOsPaymentController extends LocalizedControllerSupport {
    private final PayOsPaymentService paymentService;
    private final MemberRepository memberRepository;
    private final StaffRepository staffRepository;

    public LibrarianPayOsPaymentController(PayOsPaymentService paymentService,
            MemberRepository memberRepository,
            StaffRepository staffRepository) {
        this.paymentService = paymentService;
        this.memberRepository = memberRepository;
        this.staffRepository = staffRepository;
    }

    @PostMapping("/top-up")
    public String createTopUp(@RequestParam String memberPhone,
            @RequestParam BigDecimal amount,
            @RequestParam String requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Member member = findMember(memberPhone);
            Staff staff = requireStaff(userDetails);
            PayOsPayment payment = paymentService.createTopUpForLibrarian(member, amount, requestId, staff);
            return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", readableMessage(e));
            redirectAttributes.addFlashAttribute("memberPhone", memberPhone);
            redirectAttributes.addFlashAttribute("amount", amount);
            return "redirect:/librarian/members/topup";
        }
    }

    @PostMapping("/fine/{fineId}")
    public String createFinePayment(@PathVariable Integer fineId,
            RedirectAttributes redirectAttributes) {
        try {
            PayOsPayment payment = paymentService.createFinePaymentForLibrarian(fineId);
            return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", readableMessage(exception));
            return "redirect:/librarian/members/fines";
        }
    }

    @PostMapping("/fine/borrow/{borrowId}")
    public String createBorrowFinePayment(@PathVariable Integer borrowId,
            RedirectAttributes redirectAttributes) {
        try {
            PayOsPayment payment = paymentService.createFineBatchPaymentForLibrarian(borrowId);
            return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", readableMessage(exception));
            return "redirect:/librarian/members/fines/payment/" + borrowId;
        }
    }

    @GetMapping("/return")
    public String paymentReturn(@RequestParam(required = false) Long orderCode,
            RedirectAttributes redirectAttributes) {
        if (orderCode == null) {
            redirectAttributes.addFlashAttribute("error", message("backend.payment.orderUnknown"));
            return "redirect:/librarian/members/topup";
        }
        paymentService.getForStaff(orderCode);
        return "redirect:/librarian/payments/payos/" + orderCode;
    }

    @GetMapping("/{orderCode}")
    public String viewPayment(@PathVariable Long orderCode, Model model) {
        PayOsPayment payment = paymentService.refreshForStaff(orderCode);
        model.addAttribute("payment", payment);
        model.addAttribute("fineItems", paymentService.getFineItems(payment));
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
                ? null
                : payment.getTransaction().getTransactionId());
        return result;
    }

    @GetMapping(value = "/{orderCode}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qrImage(@PathVariable Long orderCode) {
        PayOsPayment payment = paymentService.getForStaff(orderCode);
        if (payment.getQrCode() == null || payment.getQrCode().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .body(PayOsQrImageRenderer.render(payment.getQrCode(),
                        message("backend.payment.qrPngUnsupported"), message("backend.payment.qrRenderFailed")));
    }

    private Member findMember(String lookup) {
        if (lookup == null || lookup.isBlank()) {
            throw new ValidationException(message("backend.payment.memberRequired"));
        }
        String value = lookup.trim();
        if (value.matches("\\d+")) {
            try {
                return memberRepository.findById(Integer.valueOf(value))
                        .or(() -> memberRepository.findByUserPhone(value))
                        .orElseThrow(() -> new ResourceNotFoundException(message("backend.payment.memberNotFound")));
            } catch (NumberFormatException ignored) {
                // Continue with the other lookup types below.
            }
        }
        return memberRepository.findByUserPhone(value)
                .or(() -> memberRepository.findByUserEmail(value))
                .or(() -> memberRepository.findByAccountUsername(value))
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.payment.memberNotFound")));
    }

    private String readableMessage(ApplicationException exception) {
        Throwable current = exception;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current.getMessage() == null ? message("backend.payment.qrCreateFailed") : current.getMessage();
    }

    private Staff requireStaff(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null || userDetails.getUser().getId() == null) {
            throw new ValidationException(message("backend.financial.staffRequired"));
        }
        return staffRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new ValidationException(message("backend.financial.staffRequired")));
    }
}
