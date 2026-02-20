package com.waiveliability.modules.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateFormRequest(
    @NotBlank @Size(max = 255) String name,
    String description,
    @NotNull List<FieldRequest> fields
) {}
