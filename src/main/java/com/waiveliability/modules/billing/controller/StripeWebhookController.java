package com.waiveliability.modules.billing.controller;

import com.waiveliability.modules.billing.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error handling Stripe webhook: {}", e.getClass().getSimpleName());
            // Return generic error to avoid leaking internal details
            return ResponseEntity.badRequest().body("Webhook error");
        }
    }
}
