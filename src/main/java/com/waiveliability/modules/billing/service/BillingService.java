package com.waiveliability.modules.billing.service;

import com.stripe.exception.StripeException;
import com.waiveliability.modules.billing.dto.CheckoutResponse;
import com.waiveliability.modules.billing.dto.LimitsResponse;
import com.waiveliability.modules.billing.dto.LimitsResponse.LimitInfo;
import com.waiveliability.modules.billing.dto.SubscriptionResponse;
import com.waiveliability.modules.billing.domain.Subscription;
import com.waiveliability.modules.billing.repository.SubscriptionRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final StripeService stripeService;
    private final SubscriptionRepository subscriptionRepository;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;

    private static final int FREE_FORMS_LIMIT = 3;
    private static final int FREE_SUBMISSIONS_LIMIT = 100;
    private static final int BASIC_FORMS_LIMIT = 10;
    private static final int BASIC_SUBMISSIONS_LIMIT = 1000;
    private static final int PREMIUM_FORMS_LIMIT = -1; // unlimited
    private static final int PREMIUM_SUBMISSIONS_LIMIT = -1; // unlimited

    public CheckoutResponse createCheckoutSession(UUID tenantId, String priceId) throws StripeException {
        return stripeService.createCheckoutSession(tenantId, priceId);
    }

    public CheckoutResponse createPortalSession(UUID tenantId) throws StripeException {
        return stripeService.createPortalSession(tenantId);
    }

    public SubscriptionResponse getSubscription(UUID tenantId) {
        stripeService.ensureSubscriptionExists(tenantId);
        return stripeService.getSubscription(tenantId);
    }

    public LimitsResponse getLimits(UUID tenantId) {
        stripeService.ensureSubscriptionExists(tenantId);

        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("Subscription not found"));

        int formsUsed = formRepository.countByTenantId(tenantId);
        int submissionsUsed = (int) submissionRepository.countByTenantId(tenantId);

        Subscription.PlanType plan = subscription.getPlan();
        int formsLimit = getFormsLimit(plan);
        int submissionsLimit = getSubmissionsLimit(plan);

        return LimitsResponse.builder()
                .forms(LimitInfo.builder().used(formsUsed).limit(formsLimit).build())
                .submissions(LimitInfo.builder().used(submissionsUsed).limit(submissionsLimit).build())
                .build();
    }

    public int getFormsLimit(Subscription.PlanType plan) {
        if (plan == null) return FREE_FORMS_LIMIT;
        return switch (plan) {
            case free -> FREE_FORMS_LIMIT;
            case basic -> BASIC_FORMS_LIMIT;
            case premium -> PREMIUM_FORMS_LIMIT;
        };
    }

    public int getSubmissionsLimit(Subscription.PlanType plan) {
        if (plan == null) return FREE_SUBMISSIONS_LIMIT;
        return switch (plan) {
            case free -> FREE_SUBMISSIONS_LIMIT;
            case basic -> BASIC_SUBMISSIONS_LIMIT;
            case premium -> PREMIUM_SUBMISSIONS_LIMIT;
        };
    }
}
