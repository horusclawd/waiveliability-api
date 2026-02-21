package com.waiveliability.modules.billing.controller;

import com.stripe.exception.StripeException;
import com.waiveliability.modules.billing.dto.CheckoutRequest;
import com.waiveliability.modules.billing.dto.CheckoutResponse;
import com.waiveliability.modules.billing.dto.LimitsResponse;
import com.waiveliability.modules.billing.dto.SubscriptionResponse;
import com.waiveliability.modules.billing.service.BillingService;
import com.waiveliability.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(@RequestBody(required = false) CheckoutRequest request) throws StripeException {
        String priceId = request != null ? request.getPriceId() : null;
        CheckoutResponse response = billingService.createCheckoutSession(TenantContext.current(), priceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/portal")
    public ResponseEntity<CheckoutResponse> createPortal() throws StripeException {
        CheckoutResponse response = billingService.createPortalSession(TenantContext.current());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> getSubscription() {
        SubscriptionResponse response = billingService.getSubscription(TenantContext.current());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/limits")
    public ResponseEntity<LimitsResponse> getLimits() {
        LimitsResponse response = billingService.getLimits(TenantContext.current());
        return ResponseEntity.ok(response);
    }
}
