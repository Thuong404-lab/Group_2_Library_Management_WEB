package com.lms.controller;

import com.lms.service.PayOsPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;

import java.util.Map;

@RestController
public class PayOsWebhookController {
    private final PayOsPaymentService payOsPaymentService;

    public PayOsWebhookController(PayOsPaymentService payOsPaymentService) {
        this.payOsPaymentService = payOsPaymentService;
    }

    @PostMapping("/api/payments/payos/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestBody Webhook webhook) {
        try {
            payOsPaymentService.handleWebhook(webhook);
            return ResponseEntity.ok(Map.of("message", "Webhook processed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid webhook"));
        }
    }
}
