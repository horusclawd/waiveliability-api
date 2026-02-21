package com.waiveliability.modules.notification.service;

import com.waiveliability.config.EmailConfig;
import com.waiveliability.modules.billing.domain.Subscription;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.submissions.domain.Submission;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    /**
     * Send submission confirmation to the submitter
     */
    @Async
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendSubmissionConfirmation(Submission submission, Tenant tenant) {
        if (submission.getSubmitterEmail() == null || submission.getSubmitterEmail().isBlank()) {
            log.debug("No submitter email, skipping confirmation email");
            return;
        }

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("submitterName", submission.getSubmitterName() != null ? submission.getSubmitterName() : "Valued Customer");
            model.put("formName", submission.getForm().getName());
            model.put("tenantName", tenant.getName());
            model.put("submittedAt", submission.getSubmittedAt());
            model.put("submissionId", submission.getId());

            sendEmail(
                submission.getSubmitterEmail(),
                "We Received Your Submission - " + tenant.getName(),
                "submission-confirmation.html",
                model
            );
            log.info("Sent submission confirmation to {} for submission {}", submission.getSubmitterEmail(), submission.getId());
        } catch (RuntimeException e) {
            log.error("Failed to send submission confirmation email to {}", submission.getSubmitterEmail(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to send submission confirmation email to {}", submission.getSubmitterEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send new submission alert to tenant admin (only for Basic+ plans)
     */
    @Async
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendNewSubmissionAlert(Submission submission, Tenant tenant, Subscription.PlanType plan) {
        // Plan gate: only send for Basic+ plans
        if (!isBasicOrHigher(plan)) {
            log.debug("Tenant {} is on {} plan, skipping new submission alert", tenant.getSlug(), plan);
            return;
        }

        if (!Boolean.TRUE.equals(tenant.getNotificationsEnabled()) ||
            tenant.getNotificationEmail() == null ||
            tenant.getNotificationEmail().isBlank()) {
            log.debug("Notifications not enabled for tenant {} or no notification email set", tenant.getSlug());
            return;
        }

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("tenantName", tenant.getName());
            model.put("formName", submission.getForm().getName());
            model.put("submitterName", submission.getSubmitterName() != null ? submission.getSubmitterName() : "Not provided");
            model.put("submitterEmail", submission.getSubmitterEmail() != null ? submission.getSubmitterEmail() : "Not provided");
            model.put("submittedAt", submission.getSubmittedAt());
            model.put("submissionId", submission.getId());
            model.put("statusUrl", baseUrl + "/admin/submissions/" + submission.getId());

            sendEmail(
                tenant.getNotificationEmail(),
                "New Form Submission - " + submission.getForm().getName(),
                "new-submission-alert.html",
                model
            );
            log.info("Sent new submission alert to {} for submission {}", tenant.getNotificationEmail(), submission.getId());
        } catch (RuntimeException e) {
            log.error("Failed to send new submission alert email to {}", tenant.getNotificationEmail(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to send new submission alert email to {}", tenant.getNotificationEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send password reset email to user
     */
    @Async
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendPasswordReset(String email, String resetToken, String tenantName) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("email", email);
            model.put("tenantName", tenantName);
            model.put("resetToken", resetToken);
            model.put("expiryHours", 24);
            model.put("resetUrl", baseUrl + "/auth/reset-password?token=" + resetToken);

            sendEmail(
                email,
                "Reset Your Password - " + tenantName,
                "password-reset.html",
                model
            );
            log.info("Sent password reset email to {}", email);
        } catch (RuntimeException e) {
            log.error("Failed to send password reset email to {}", email, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send plan upgrade notification to tenant admin
     */
    @Async
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendPlanUpgradeNotification(Tenant tenant, String oldPlan, String newPlan) {
        if (tenant.getNotificationEmail() == null || tenant.getNotificationEmail().isBlank()) {
            log.debug("No notification email for tenant {}, skipping upgrade notification", tenant.getSlug());
            return;
        }

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("tenantName", tenant.getName());
            model.put("oldPlan", oldPlan);
            model.put("newPlan", newPlan);
            model.put("upgradeDate", java.time.Instant.now());
            model.put("featuresUrl", baseUrl + "/admin/billing");

            sendEmail(
                tenant.getNotificationEmail(),
                "Your Plan Has Been Upgraded - " + tenant.getName(),
                "plan-upgrade.html",
                model
            );
            log.info("Sent plan upgrade notification to {} for tenant {}", tenant.getNotificationEmail(), tenant.getSlug());
        } catch (RuntimeException e) {
            log.error("Failed to send plan upgrade notification email to {}", tenant.getNotificationEmail(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to send plan upgrade notification email to {}", tenant.getNotificationEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendEmail(String to, String subject, String templateName, Map<String, Object> model) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(emailConfig.getEmailFrom());

        Context context = new Context();
        context.setVariables(model);
        String htmlContent = templateEngine.process(templateName, context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private boolean isBasicOrHigher(Subscription.PlanType plan) {
        if (plan == null) return false;
        return plan == Subscription.PlanType.basic || plan == Subscription.PlanType.premium;
    }
}
