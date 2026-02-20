package com.waiveliability.modules.business.dto;

public record UpdateBrandingRequest(
    String primaryColor,
    String bgColor,
    String fontFamily,
    boolean hidePoweredBy
) {}
