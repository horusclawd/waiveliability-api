package com.waiveliability.modules.forms.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FormResponse(
    UUID id,
    String name,
    String description,
    String status,
    List<FormFieldResponse> fields,
    Instant createdAt,
    Instant updatedAt
) {}
