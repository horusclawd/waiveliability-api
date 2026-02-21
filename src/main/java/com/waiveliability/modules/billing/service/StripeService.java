package com.waiveliability.modules.billing.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.waiveliability.config.StripeConfig;
import com.waiveliability.modules.billing.domain.Subscription;
import com.waiveliability.modules.billing.domain.Subscription.PlanType;
import com.waiveliability.modules.billing.domain.Subscription.SubscriptionStatus;
import com.waiveliability.modules.billing.dto.CheckoutResponse;
import com.waiveliability.modules.billing.dto.SubscriptionResponse;
import com.waiveliability.modules.billing.repository.SubscriptionRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeConfig stripeConfig;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;

    private static final String DEFAULT_PRICE_BASIC = "price_basic_monthly";
    private static final String DEFAULT_PRICE_PREMIUM = "price_premium_monthly";

    @Transactional
    public CheckoutResponse createCheckoutSession(UUID tenantId, String priceId) throws StripeException {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        String customerId = getOrCreateCustomer(tenant);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl("https://waiveliability.com/billing/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://waiveliability.com/billing/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId != null ? priceId : DEFAULT_PRICE_BASIC)
                        .setQuantity(1L)
                        .build())
                .build();

        Session session = Session.create(params);

        return CheckoutResponse.builder()
                .url(session.getUrl())
                .build();
    }

    @Transactional
    public CheckoutResponse createPortalSession(UUID tenantId) throws StripeException {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found"));

        if (subscription.getStripeCustomerId() == null) {
            throw new IllegalArgumentException("No Stripe customer ID found");
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(subscription.getStripeCustomerId())
                .setReturnUrl("https://waiveliability.com/billing")
                .build();

        com.stripe.model.billingportal.Session portalSession =
                com.stripe.model.billingportal.Session.create(params);

        return CheckoutResponse.builder()
                .url(portalSession.getUrl())
                .build();
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed", e);
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.payment_failed" -> handlePaymentFailed(event);
            default -> log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new IllegalArgumentException("Cannot deserialize checkout session"));

        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        Subscription subscription = subscriptionRepository.findByStripeCustomerId(customerId)
                .orElseGet(() -> {
                    // Find tenant by looking up subscription via Stripe metadata if needed
                    // For now, this handler assumes the subscription was already created
                    return null;
                });

        if (subscription != null) {
            subscription.setStripeSubscriptionId(subscriptionId);
            subscription.setStatus(SubscriptionStatus.active);
            subscription.setCurrentPeriodStart(Instant.now());
            subscription.setCurrentPeriodEnd(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
            subscriptionRepository.save(subscription);
            log.info("Subscription activated: {}", subscriptionId);
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSubscription = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new IllegalArgumentException("Cannot deserialize subscription"));

        String subscriptionId = stripeSubscription.getId();
        String status = stripeSubscription.getStatus();
        long periodEnd = stripeSubscription.getCurrentPeriodEnd();

        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(subscription -> {
                    subscription.setStatus(mapStripeStatus(status));
                    subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
                    subscriptionRepository.save(subscription);
                    log.info("Subscription updated: {}", subscriptionId);
                });
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new IllegalArgumentException("Cannot deserialize subscription"));

        String subscriptionId = stripeSubscription.getId();

        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.canceled);
                    subscription.setPlan(PlanType.free);
                    subscription.setStripeSubscriptionId(null);
                    subscriptionRepository.save(subscription);
                    log.info("Subscription canceled: {}", subscriptionId);
                });
    }

    private void handlePaymentFailed(Event event) {
        com.stripe.model.Invoice invoice = (com.stripe.model.Invoice)
                event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new IllegalArgumentException("Cannot deserialize invoice"));

        String subscriptionId = invoice.getSubscription();
        if (subscriptionId != null) {
            subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                    .ifPresent(subscription -> {
                        subscription.setStatus(SubscriptionStatus.past_due);
                        subscriptionRepository.save(subscription);
                        log.warn("Payment failed for subscription: {}", subscriptionId);
                    });
        }
    }

    public SubscriptionResponse getSubscription(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .map(sub -> SubscriptionResponse.builder()
                        .status(sub.getStatus().name())
                        .plan(sub.getPlan().name())
                        .currentPeriodEnd(sub.getCurrentPeriodEnd())
                        .build())
                .orElseGet(() -> SubscriptionResponse.builder()
                        .status("active")
                        .plan("free")
                        .currentPeriodEnd(null)
                        .build());
    }

    public String getOrCreateCustomer(Tenant tenant) {
        return subscriptionRepository.findByTenantId(tenant.getId())
                .map(Subscription::getStripeCustomerId)
                .orElseGet(() -> createCustomer(tenant));
    }

    private String createCustomer(Tenant tenant) {
        try {
            Customer customer = Customer.create(
                    com.stripe.param.CustomerCreateParams.builder()
                            .setEmail(tenant.getSlug() + "@waiveliability.com")
                            .setName(tenant.getName())
                            .build()
            );
            return customer.getId();
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer", e);
            throw new RuntimeException("Failed to create Stripe customer", e);
        }
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.active;
            case "canceled" -> SubscriptionStatus.canceled;
            case "past_due", "unpaid" -> SubscriptionStatus.past_due;
            default -> SubscriptionStatus.past_due;
        };
    }

    public void ensureSubscriptionExists(UUID tenantId) {
        if (!subscriptionRepository.existsByTenantId(tenantId)) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

            Subscription subscription = Subscription.builder()
                    .tenant(tenant)
                    .status(SubscriptionStatus.active)
                    .plan(PlanType.free)
                    .currentPeriodStart(Instant.now())
                    .currentPeriodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                    .build();

            subscriptionRepository.save(subscription);
            log.info("Created default free subscription for tenant: {}", tenantId);
        }
    }

    public Subscription getOrCreateSubscription(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

                    Subscription subscription = Subscription.builder()
                            .tenant(tenant)
                            .status(SubscriptionStatus.active)
                            .plan(PlanType.free)
                            .currentPeriodStart(Instant.now())
                            .currentPeriodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                            .build();

                    return subscriptionRepository.save(subscription);
                });
    }
}
