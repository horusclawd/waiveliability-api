package com.waiveliability.modules.business.dto;

public record PublicBrandingResponse(
    String tenantName,
    String primaryColor,
    String bgColor,
    String fontFamily,
    String logoUrl,
    boolean hidePoweredBy
) {}
