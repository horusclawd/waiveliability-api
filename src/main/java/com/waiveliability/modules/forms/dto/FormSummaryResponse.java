package com.waiveliability.modules.forms.dto;

import java.time.Instant;
import java.util.UUID;

public record FormSummaryResponse(
    UUID id,
    String name,
    String description,
    String status,
    int fieldCount,
    Instant createdAt,
    Instant updatedAt
) {}
