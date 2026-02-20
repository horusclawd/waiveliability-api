package com.waiveliability.modules.business.dto;

import java.time.Instant;
import java.util.UUID;

public record BusinessResponse(
    UUID id,
    String name,
    String slug,
    String plan,
    String address,
    String phone,
    String websiteUrl,
    BrandingInfo branding,
    Instant createdAt,
    Instant updatedAt
) {
    public record BrandingInfo(
        String primaryColor,
        String bgColor,
        String fontFamily,
        String logoUrl,
        boolean hidePoweredBy
    ) {}
}
