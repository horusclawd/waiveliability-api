package com.waiveliability.modules.forms.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record FieldRequest(
    UUID id,
    @NotBlank String fieldType,
    @NotBlank String label,
    String placeholder,
    boolean required,
    int fieldOrder,
    List<FormFieldOption> options
) {}
