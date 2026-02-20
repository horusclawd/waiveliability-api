package com.waiveliability.modules.forms.dto;

import java.util.List;
import java.util.UUID;

public record FormFieldResponse(
    UUID id,
    String fieldType,
    String label,
    String placeholder,
    boolean required,
    int fieldOrder,
    List<FormFieldOption> options
) {}
