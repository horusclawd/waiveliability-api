package com.waiveliability.modules.notification.dto;

import jakarta.validation.constraints.Email;

public record UpdateNotificationSettingsRequest(
    Boolean notificationsEnabled,

    @Email(message = "Invalid email format")
    String notificationEmail
) {}
