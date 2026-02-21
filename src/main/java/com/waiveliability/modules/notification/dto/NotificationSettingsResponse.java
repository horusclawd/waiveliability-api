package com.waiveliability.modules.notification.dto;

public record NotificationSettingsResponse(
    boolean notificationsEnabled,
    String notificationEmail
) {}
