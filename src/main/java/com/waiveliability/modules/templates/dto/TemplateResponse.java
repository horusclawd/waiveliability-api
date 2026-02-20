package com.waiveliability.modules.templates.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
    UUID id,
    String name,
    String description,
    String category,
    boolean isPremium,
    long usageCount,
    List<TemplateFieldResponse> fields,
    Instant createdAt,
    Instant updatedAt
) {}
