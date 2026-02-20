package com.waiveliability.modules.business.controller;

import com.waiveliability.modules.business.dto.BusinessResponse;
import com.waiveliability.modules.business.dto.UpdateBrandingRequest;
import com.waiveliability.modules.business.dto.UpdateBusinessRequest;
import com.waiveliability.modules.business.service.BusinessService;
import com.waiveliability.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    public ResponseEntity<BusinessResponse> getBusiness(
        @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(businessService.getBusiness(TenantContext.current()));
    }

    @PutMapping
    public ResponseEntity<BusinessResponse> updateBusiness(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateBusinessRequest request
    ) {
        return ResponseEntity.ok(businessService.updateBusiness(TenantContext.current(), request));
    }

    @PatchMapping("/branding")
    public ResponseEntity<BusinessResponse> updateBranding(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateBrandingRequest request
    ) {
        return ResponseEntity.ok(businessService.updateBranding(TenantContext.current(), request));
    }

    @PostMapping("/logo")
    public ResponseEntity<BusinessResponse> uploadLogo(
        @AuthenticationPrincipal String userId,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(businessService.uploadLogo(TenantContext.current(), file));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<Void> deleteLogo(
        @AuthenticationPrincipal String userId
    ) {
        businessService.deleteLogo(TenantContext.current());
        return ResponseEntity.noContent().build();
    }
}
