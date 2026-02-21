package com.waiveliability.modules.billing.service;

import com.waiveliability.modules.billing.domain.Subscription;
import com.waiveliability.modules.billing.domain.Subscription.PlanType;
import com.waiveliability.modules.billing.domain.Subscription.SubscriptionStatus;
import com.waiveliability.modules.billing.dto.CheckoutResponse;
import com.waiveliability.modules.billing.dto.LimitsResponse;
import com.waiveliability.modules.billing.dto.SubscriptionResponse;
import com.waiveliability.modules.billing.repository.SubscriptionRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private FormRepository formRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private BillingService billingService;

    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = Tenant.builder()
                .id(tenantId)
                .name("Test Tenant")
                .slug("test-tenant")
                .build();
    }

    @Test
    void createCheckoutSession_shouldCallStripeService() throws Exception {
        // Arrange
        String priceId = "price_123";
        CheckoutResponse expectedResponse = CheckoutResponse.builder()
                .url("https://checkout.stripe.com/test")
                .build();
        when(stripeService.createCheckoutSession(tenantId, priceId)).thenReturn(expectedResponse);

        // Act
        CheckoutResponse response = billingService.createCheckoutSession(tenantId, priceId);

        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse.getUrl(), response.getUrl());
        verify(stripeService).createCheckoutSession(tenantId, priceId);
    }

    @Test
    void getSubscription_shouldReturnFreeWhenNoSubscription() {
        // Arrange
        doNothing().when(stripeService).ensureSubscriptionExists(tenantId);
        when(stripeService.getSubscription(tenantId)).thenReturn(
                SubscriptionResponse.builder()
                        .status("active")
                        .plan("free")
                        .build()
        );

        // Act
        SubscriptionResponse response = billingService.getSubscription(tenantId);

        // Assert
        assertNotNull(response);
        assertEquals("active", response.getStatus());
        assertEquals("free", response.getPlan());
        verify(stripeService).ensureSubscriptionExists(tenantId);
    }

    @Test
    void getLimits_shouldReturnCorrectLimitsForFreePlan() {
        // Arrange
        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .status(SubscriptionStatus.active)
                .plan(PlanType.free)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60))
                .build();

        doNothing().when(stripeService).ensureSubscriptionExists(tenantId);
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(2);
        when(submissionRepository.countByTenantId(tenantId)).thenReturn(50L);

        // Act
        LimitsResponse response = billingService.getLimits(tenantId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getForms());
        assertEquals(2, response.getForms().getUsed());
        assertEquals(3, response.getForms().getLimit());
        assertNotNull(response.getSubmissions());
        assertEquals(50, response.getSubmissions().getUsed());
        assertEquals(100, response.getSubmissions().getLimit());
    }

    @Test
    void getLimits_shouldReturnCorrectLimitsForBasicPlan() {
        // Arrange
        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .status(SubscriptionStatus.active)
                .plan(PlanType.basic)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60))
                .build();

        doNothing().when(stripeService).ensureSubscriptionExists(tenantId);
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(5);
        when(submissionRepository.countByTenantId(tenantId)).thenReturn(500L);

        // Act
        LimitsResponse response = billingService.getLimits(tenantId);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getForms().getUsed());
        assertEquals(10, response.getForms().getLimit());
        assertEquals(500, response.getSubmissions().getUsed());
        assertEquals(1000, response.getSubmissions().getLimit());
    }

    @Test
    void getLimits_shouldReturnUnlimitedForPremiumPlan() {
        // Arrange
        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .status(SubscriptionStatus.active)
                .plan(PlanType.premium)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60))
                .build();

        doNothing().when(stripeService).ensureSubscriptionExists(tenantId);
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(100);
        when(submissionRepository.countByTenantId(tenantId)).thenReturn(10000L);

        // Act
        LimitsResponse response = billingService.getLimits(tenantId);

        // Assert
        assertNotNull(response);
        assertEquals(100, response.getForms().getUsed());
        assertEquals(-1, response.getForms().getLimit()); // unlimited
        assertEquals(10000, response.getSubmissions().getUsed());
        assertEquals(-1, response.getSubmissions().getLimit()); // unlimited
    }

    @Test
    void getFormsLimit_shouldReturnCorrectLimits() {
        // Free plan
        assertEquals(3, billingService.getFormsLimit(PlanType.free));

        // Basic plan
        assertEquals(10, billingService.getFormsLimit(PlanType.basic));

        // Premium plan
        assertEquals(-1, billingService.getFormsLimit(PlanType.premium));

        // Null plan defaults to free
        assertEquals(3, billingService.getFormsLimit(null));
    }

    @Test
    void getSubmissionsLimit_shouldReturnCorrectLimits() {
        // Free plan
        assertEquals(100, billingService.getSubmissionsLimit(PlanType.free));

        // Basic plan
        assertEquals(1000, billingService.getSubmissionsLimit(PlanType.basic));

        // Premium plan
        assertEquals(-1, billingService.getSubmissionsLimit(PlanType.premium));

        // Null plan defaults to free
        assertEquals(100, billingService.getSubmissionsLimit(null));
    }
}
