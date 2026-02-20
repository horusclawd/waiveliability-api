package com.waiveliability.modules.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFormRequest(
    @NotBlank @Size(max = 255) String name,
    String description
) {}
