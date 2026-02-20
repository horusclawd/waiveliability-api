package com.waiveliability.modules.submissions.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmissionResponse(
    UUID id,
    UUID formId,
    String submitterName,
    String submitterEmail,
    Map<String, Object> formData,
    String signatureUrl,
    String status,
    Instant submittedAt
) {}
