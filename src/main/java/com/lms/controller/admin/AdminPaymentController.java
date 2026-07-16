package com.lms.controller.admin;

import com.lms.dto.request.PaymentSearchCriteria;
import com.lms.dto.response.ReportExport;
import com.lms.entity.PayOsPayment;
import com.lms.service.AdminPaymentService;
import com.lms.service.PayOsPaymentAuditService;
import com.lms.service.PayOsPaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {
    private final AdminPaymentService adminPaymentService;
    private final PayOsPaymentService paymentService;
    private final PayOsPaymentAuditService auditService;

    public AdminPaymentController(AdminPaymentService adminPaymentService,
                                  PayOsPaymentService paymentService,
                                  PayOsPaymentAuditService auditService) {
        this.adminPaymentService = adminPaymentService;
        this.paymentService = paymentService;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String purpose,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int issuePage,
                       Model model) {
        PaymentSearchCriteria criteria = new PaymentSearchCriteria(keyword, status, purpose, fromDate, toDate);
        model.addAttribute("payments", adminPaymentService.searchPayments(criteria, page));
        model.addAttribute("openIssues", adminPaymentService.getOpenIssues(issuePage));
        model.addAttribute("openIssueCount", adminPaymentService.countOpenIssues());
        model.addAttribute("criteria", criteria);
        model.addAttribute("statuses", List.of("PENDING", "PAID", "EXPIRED", "CANCELLED", "FAILED"));
        model.addAttribute("purposes", List.of("TOP_UP", "FINE", "FINE_BATCH", "BORROW_FEE"));
        return "admin/payments";
    }

    @GetMapping("/{orderCode}")
    public String detail(@PathVariable Long orderCode,
                         @RequestParam(defaultValue = "0") int auditPage,
                         Model model) {
        PayOsPayment payment = adminPaymentService.getPayment(orderCode);
        model.addAttribute("payment", payment);
        model.addAttribute("audits", adminPaymentService.getPaymentAudits(payment.getPaymentId(), auditPage));
        model.addAttribute("issues", adminPaymentService.getPaymentIssues(payment.getPaymentId()));
        return "admin/payment-detail";
    }

    @GetMapping("/{orderCode}/status")
    @ResponseBody
    public Map<String, Object> status(@PathVariable Long orderCode) {
        PayOsPayment payment = paymentService.refreshForStaff(orderCode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderCode", payment.getOrderCode());
        result.put("status", payment.getStatus());
        result.put("paidAt", payment.getPaidAt());
        result.put("transactionId", payment.getTransaction() == null ? null : payment.getTransaction().getTransactionId());
        return result;
    }

    @PostMapping("/{orderCode}/reconcile")
    public String reconcile(@PathVariable Long orderCode, RedirectAttributes redirectAttributes) {
        PayOsPayment payment = adminPaymentService.getPayment(orderCode);
        auditService.record(payment, "RECONCILIATION_REQUESTED", "ADMIN", payment.getStatus(), payment.getStatus(), true,
                "Quản trị viên yêu cầu đối soát thủ công.");
        try {
            PayOsPayment refreshed = paymentService.reconcileForStaff(orderCode);
            auditService.resolveReconciliationIssue(orderCode, "ADMIN");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đối soát hoàn tất. Trạng thái hiện tại: " + refreshed.getStatus());
        } catch (Exception exception) {
            auditService.recordReconciliationFailure(orderCode, exception.getMessage(), "ADMIN");
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/payments/" + orderCode;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPayments(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String purpose,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                  @RequestParam(defaultValue = "csv") String format) {
        return download(adminPaymentService.exportPayments(
                new PaymentSearchCriteria(keyword, status, purpose, fromDate, toDate), format));
    }

    @GetMapping("/audit-logs")
    public String auditLogs(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String eventType,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                            Model model) {
        model.addAttribute("audits", adminPaymentService.searchAudits(keyword, eventType, fromDate, toDate, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("eventType", eventType);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "admin/payment-audit-logs";
    }

    @GetMapping("/audit-logs/export")
    public ResponseEntity<byte[]> exportAudits(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String eventType,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                @RequestParam(defaultValue = "csv") String format) {
        return download(adminPaymentService.exportAudits(keyword, eventType, fromDate, toDate, format));
    }

    private ResponseEntity<byte[]> download(ReportExport export) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.getFileName()).build().toString())
                .header(HttpHeaders.CONTENT_TYPE, export.getContentType())
                .body(export.getContent());
    }
}
