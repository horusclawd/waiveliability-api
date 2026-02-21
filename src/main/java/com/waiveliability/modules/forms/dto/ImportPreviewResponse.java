package com.waiveliability.modules.forms.dto;

import java.util.List;

public record ImportPreviewResponse(
    String filename,
    String extractedText,
    List<DetectedField> fields
) {}
