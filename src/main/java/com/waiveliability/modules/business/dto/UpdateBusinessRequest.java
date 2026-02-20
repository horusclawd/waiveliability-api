package com.waiveliability.modules.business.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBusinessRequest(
    @NotBlank String name,
    String address,
    String phone,
    String websiteUrl
) {}
