package com.waiveliability.security;

import com.waiveliability.common.exception.PlanLimitExceededException;
import com.waiveliability.modules.billing.domain.Subscription;
import com.waiveliability.modules.billing.domain.Subscription.PlanType;
import com.waiveliability.modules.billing.domain.Subscription.SubscriptionStatus;
import com.waiveliability.modules.billing.repository.SubscriptionRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
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
class PlanEnforcerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private FormRepository formRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private PlanEnforcer planEnforcer;

    private UUID tenantId;
    private Tenant tenant;
    private Subscription freeSubscription;
    private Subscription basicSubscription;
    private Subscription premiumSubscription;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = Tenant.builder()
                .id(tenantId)
                .name("Test Tenant")
                .slug("test-tenant")
                .build();

        freeSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .status(SubscriptionStatus.active)
                .plan(PlanType.free)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60))
                .build();

        basicSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .status(SubscriptionStatus.active)
                .plan(PlanType.basic)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60))
                .build();

        premiumSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .status(SubscriptionStatus.active)
                .plan(PlanType.premium)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60))
                .build();
    }

    @Test
    void checkFormLimit_shouldThrowExceptionWhenFreePlanLimitExceeded() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(freeSubscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(3); // Already at limit

        // Act & Assert
        assertThrows(PlanLimitExceededException.class, () -> {
            planEnforcer.checkFormLimit(tenantId);
        });
    }

    @Test
    void checkFormLimit_shouldAllowWhenFreePlanUnderLimit() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(freeSubscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(2); // Under limit

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            planEnforcer.checkFormLimit(tenantId);
        });
    }

    @Test
    void checkFormLimit_shouldAllowBasicPlanUnderLimit() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(basicSubscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(9); // Under limit for basic (10)

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            planEnforcer.checkFormLimit(tenantId);
        });
    }

    @Test
    void checkFormLimit_shouldAllowPremiumPlanUnlimited() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(premiumSubscription));
        when(formRepository.countByTenantId(tenantId)).thenReturn(10000); // Way over limits

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            planEnforcer.checkFormLimit(tenantId);
        });
    }

    @Test
    void checkSubmissionLimit_shouldThrowExceptionWhenFreePlanLimitExceeded() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(freeSubscription));
        when(submissionRepository.countByTenantId(tenantId)).thenReturn(100L); // At limit

        // Act & Assert
        assertThrows(PlanLimitExceededException.class, () -> {
            planEnforcer.checkSubmissionLimit(tenantId);
        });
    }

    @Test
    void checkSubmissionLimit_shouldAllowWhenUnderLimit() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(freeSubscription));
        when(submissionRepository.countByTenantId(tenantId)).thenReturn(50L); // Under limit

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            planEnforcer.checkSubmissionLimit(tenantId);
        });
    }

    @Test
    void checkSubmissionLimit_shouldAllowPremiumPlanUnlimited() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(premiumSubscription));
        when(submissionRepository.countByTenantId(tenantId)).thenReturn(100000L); // Way over limits

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            planEnforcer.checkSubmissionLimit(tenantId);
        });
    }

    @Test
    void checkFormLimit_shouldCreateDefaultSubscriptionWhenNotExists() {
        // Arrange
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        when(formRepository.countByTenantId(tenantId)).thenReturn(2);

        // Act & Assert - should not throw and create default subscription
        assertDoesNotThrow(() -> {
            planEnforcer.checkFormLimit(tenantId);
        });
        verify(subscriptionRepository).save(any(Subscription.class));
    }
}
