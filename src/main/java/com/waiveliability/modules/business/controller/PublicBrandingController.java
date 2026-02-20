package com.waiveliability.modules.business.controller;

import com.waiveliability.modules.business.dto.PublicBrandingResponse;
import com.waiveliability.modules.business.service.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicBrandingController {

    private final BusinessService businessService;

    @GetMapping("/{tenantSlug}/branding")
    public ResponseEntity<PublicBrandingResponse> getPublicBranding(
        @PathVariable String tenantSlug
    ) {
        return ResponseEntity.ok(businessService.getPublicBranding(tenantSlug));
    }
}
