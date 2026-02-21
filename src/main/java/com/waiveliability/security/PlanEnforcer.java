package com.waiveliability.security;

import com.waiveliability.common.exception.PlanLimitExceededException;
import com.waiveliability.modules.billing.domain.Subscription;
import com.waiveliability.modules.billing.domain.Subscription.PlanType;
import com.waiveliability.modules.billing.repository.SubscriptionRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * AOP aspect that enforces plan limits declaratively via @CheckPlanLimit.
 * Implemented in Sprint 8 with full billing module.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanEnforcer {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;

    private static final int FREE_FORMS_LIMIT = 3;
    private static final int FREE_SUBMISSIONS_LIMIT = 100;
    private static final int BASIC_FORMS_LIMIT = 10;
    private static final int BASIC_SUBMISSIONS_LIMIT = 1000;

    @Before("@annotation(checkPlanLimit)")
    public void enforce(JoinPoint joinPoint, CheckPlanLimit checkPlanLimit) {
        PlanFeature feature = checkPlanLimit.feature();
        UUID tenantId = TenantContext.current();

        if (tenantId == null) {
            log.debug("No tenant context, allowing feature: {}", feature);
            return;
        }

        log.debug("Checking plan limit for feature: {}, tenant: {}", feature, tenantId);

        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        PlanType plan = subscription.getPlan();

        // Check feature-specific limits
        switch (feature) {
            case CUSTOM_BRANDING -> enforceBasicOrHigher(plan, feature);
            case SUBMISSION_EXPORT -> enforcePremium(plan, feature);
            case ANALYTICS -> enforcePremium(plan, feature);
            case TEAM_MEMBERS -> enforcePremium(plan, feature);
            case SUBMISSION_ALERTS -> enforceBasicOrHigher(plan, feature);
            case UNLIMITED_FORMS -> enforcePremium(plan, feature);
            case UNLIMITED_SUBMISSIONS -> enforcePremium(plan, feature);
            case CUSTOM_DOMAIN -> enforceEnterprise(plan, feature);
            case DOCUMENT_IMPORT -> enforceBasicOrHigher(plan, feature);
            default -> log.debug("No limit check for feature: {}", feature);
        }
    }

    private void enforceBasicOrHigher(PlanType plan, PlanFeature feature) {
        if (plan == PlanType.free) {
            throw new PlanLimitExceededException(feature.name());
        }
    }

    private void enforcePremium(PlanType plan, PlanFeature feature) {
        if (plan != PlanType.premium) {
            throw new PlanLimitExceededException(feature.name());
        }
    }

    private void enforceEnterprise(PlanType plan, PlanFeature feature) {
        // For now, Enterprise is not implemented - only free/basic/premium
        // This would require custom domain setup in Sprint 9+
        throw new PlanLimitExceededException(feature.name());
    }

    private Subscription createDefaultSubscription(UUID tenantId) {
        log.info("Creating default free subscription for tenant: {}", tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .status(Subscription.SubscriptionStatus.active)
                .plan(PlanType.free)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .build();
        return subscriptionRepository.save(subscription);
    }

    /**
     * Check if tenant can create a new form (enforce form limits).
     */
    public void checkFormLimit(UUID tenantId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        int formsUsed = formRepository.countByTenantId(tenantId);
        int limit = getFormsLimit(subscription.getPlan());

        if (limit > 0 && formsUsed >= limit) {
            throw new PlanLimitExceededException("UNLIMITED_FORMS");
        }
    }

    /**
     * Check if tenant can create a new submission (enforce submission limits).
     */
    public void checkSubmissionLimit(UUID tenantId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        long submissionsUsed = submissionRepository.countByTenantId(tenantId);
        int limit = getSubmissionsLimit(subscription.getPlan());

        if (limit > 0 && submissionsUsed >= limit) {
            throw new PlanLimitExceededException("UNLIMITED_SUBMISSIONS");
        }
    }

    private int getFormsLimit(PlanType plan) {
        if (plan == null) return FREE_FORMS_LIMIT;
        return switch (plan) {
            case free -> FREE_FORMS_LIMIT;
            case basic -> BASIC_FORMS_LIMIT;
            case premium -> Integer.MAX_VALUE;
        };
    }

    private int getSubmissionsLimit(PlanType plan) {
        if (plan == null) return FREE_SUBMISSIONS_LIMIT;
        return switch (plan) {
            case free -> FREE_SUBMISSIONS_LIMIT;
            case basic -> BASIC_SUBMISSIONS_LIMIT;
            case premium -> Integer.MAX_VALUE;
        };
    }
}
