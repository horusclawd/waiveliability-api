package com.waiveliability.modules.identity.dto;

import java.util.UUID;

public record AuthResponse(
    UUID userId,
    UUID tenantId,
    String email,
    String name,
    String role
) {}
