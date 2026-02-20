package com.waiveliability.modules.templates.dto;

import com.waiveliability.modules.forms.dto.FormFieldOption;

import java.util.List;
import java.util.UUID;

public record TemplateFieldResponse(
    UUID id,
    String fieldType,
    String label,
    String placeholder,
    boolean required,
    int fieldOrder,
    List<FormFieldOption> options
) {}
