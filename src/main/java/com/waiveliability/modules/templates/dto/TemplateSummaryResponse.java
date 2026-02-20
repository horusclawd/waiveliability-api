package com.waiveliability.modules.templates.dto;

import java.time.Instant;
import java.util.UUID;

public record TemplateSummaryResponse(
    UUID id,
    String name,
    String description,
    String category,
    boolean isPremium,
    long usageCount,
    int fieldCount,
    Instant createdAt,
    Instant updatedAt
) {}
