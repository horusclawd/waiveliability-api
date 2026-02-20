package com.waiveliability.modules.identity.dto;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
    UUID id,
    UUID tenantId,
    String email,
    String name,
    String role,
    String tenantName,
    String tenantSlug,
    String tenantPlan,
    Instant createdAt
) {}
