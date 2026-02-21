package com.waiveliability.modules.notification.controller;

import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.notification.dto.NotificationSettingsResponse;
import com.waiveliability.modules.notification.dto.UpdateNotificationSettingsRequest;
import com.waiveliability.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final TenantRepository tenantRepository;

    /**
     * GET /api/v1/admin/settings/notifications - Get notification settings for current tenant
     */
    @GetMapping("/notifications")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings() {
        UUID tenantId = TenantContext.current();
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        NotificationSettingsResponse response = new NotificationSettingsResponse(
            tenant.getNotificationsEnabled() != null && tenant.getNotificationsEnabled(),
            tenant.getNotificationEmail()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/admin/settings/notifications - Update notification settings for current tenant
     */
    @PutMapping("/notifications")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
        @Valid @RequestBody UpdateNotificationSettingsRequest request
    ) {
        UUID tenantId = TenantContext.current();
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Validate: email required if notifications enabled (handled by @Email annotation + manual check)
        if (Boolean.TRUE.equals(request.notificationsEnabled())
            && (request.notificationEmail() == null || request.notificationEmail().isBlank())) {
            throw new IllegalArgumentException("Notification email is required when notifications are enabled");
        }

        tenant.setNotificationsEnabled(request.notificationsEnabled());
        tenant.setNotificationEmail(request.notificationEmail());
        tenantRepository.save(tenant);

        NotificationSettingsResponse response = new NotificationSettingsResponse(
            tenant.getNotificationsEnabled() != null && tenant.getNotificationsEnabled(),
            tenant.getNotificationEmail()
        );

        return ResponseEntity.ok(response);
    }
}
