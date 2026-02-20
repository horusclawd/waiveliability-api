package com.waiveliability.modules.business.service;

import com.waiveliability.common.exception.ApiException;
import com.waiveliability.common.storage.S3Service;
import com.waiveliability.modules.business.domain.TenantBranding;
import com.waiveliability.modules.business.dto.BusinessResponse;
import com.waiveliability.modules.business.dto.PublicBrandingResponse;
import com.waiveliability.modules.business.dto.UpdateBrandingRequest;
import com.waiveliability.modules.business.dto.UpdateBusinessRequest;
import com.waiveliability.modules.business.repository.TenantBrandingRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.security.CheckPlanLimit;
import com.waiveliability.security.PlanFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessService {

    private static final long MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );
    private static final Duration LOGO_URL_EXPIRY = Duration.ofMinutes(15);

    private final TenantRepository tenantRepository;
    private final TenantBrandingRepository tenantBrandingRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public BusinessResponse getBusiness(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        TenantBranding branding = getOrCreateBranding(tenant);
        return toBusinessResponse(tenant, branding);
    }

    public BusinessResponse updateBusiness(UUID tenantId, UpdateBusinessRequest req) {
        Tenant tenant = requireTenant(tenantId);
        tenant.setName(req.name());
        tenant.setAddress(req.address());
        tenant.setPhone(req.phone());
        tenant.setWebsiteUrl(req.websiteUrl());
        tenantRepository.save(tenant);
        TenantBranding branding = getOrCreateBranding(tenant);
        return toBusinessResponse(tenant, branding);
    }

    @CheckPlanLimit(feature = PlanFeature.CUSTOM_BRANDING)
    public BusinessResponse updateBranding(UUID tenantId, UpdateBrandingRequest req) {
        Tenant tenant = requireTenant(tenantId);
        TenantBranding branding = getOrCreateBranding(tenant);
        branding.setPrimaryColor(req.primaryColor());
        branding.setBgColor(req.bgColor());
        branding.setFontFamily(req.fontFamily());
        branding.setHidePoweredBy(req.hidePoweredBy());
        tenantBrandingRepository.save(branding);
        return toBusinessResponse(tenant, branding);
    }

    @CheckPlanLimit(feature = PlanFeature.CUSTOM_BRANDING)
    public BusinessResponse uploadLogo(UUID tenantId, MultipartFile file) {
        Tenant tenant = requireTenant(tenantId);

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Logo must be a JPEG, PNG, or WebP image");
        }

        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Logo must be 5 MB or smaller");
        }

        TenantBranding branding = getOrCreateBranding(tenant);

        // Delete old logo if present
        if (branding.getLogoS3Key() != null) {
            s3Service.delete(branding.getLogoS3Key());
        }

        String extension = extensionFor(contentType);
        String key = "logos/" + tenantId + "/" + UUID.randomUUID() + "." + extension;

        try {
            s3Service.upload(key, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to read uploaded file", e);
        }

        branding.setLogoS3Key(key);
        tenantBrandingRepository.save(branding);
        return toBusinessResponse(tenant, branding);
    }

    public void deleteLogo(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        TenantBranding branding = getOrCreateBranding(tenant);

        if (branding.getLogoS3Key() != null) {
            s3Service.delete(branding.getLogoS3Key());
            branding.setLogoS3Key(null);
            tenantBrandingRepository.save(branding);
        }
    }

    @Transactional(readOnly = true)
    public PublicBrandingResponse getPublicBranding(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        TenantBranding branding = tenantBrandingRepository.findById(tenant.getId())
            .orElse(emptyBranding(tenant));

        String logoUrl = resolveLogoUrl(branding);

        return new PublicBrandingResponse(
            tenant.getName(),
            branding.getPrimaryColor(),
            branding.getBgColor(),
            branding.getFontFamily(),
            logoUrl,
            branding.isHidePoweredBy()
        );
    }

    // --- helpers ---

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    private TenantBranding getOrCreateBranding(Tenant tenant) {
        return tenantBrandingRepository.findById(tenant.getId())
            .orElseGet(() -> {
                TenantBranding branding = TenantBranding.builder()
                    .tenant(tenant)
                    .build();
                return tenantBrandingRepository.save(branding);
            });
    }

    private TenantBranding emptyBranding(Tenant tenant) {
        return TenantBranding.builder()
            .tenant(tenant)
            .build();
    }

    private String resolveLogoUrl(TenantBranding branding) {
        if (branding.getLogoS3Key() == null) {
            return null;
        }
        return s3Service.generateSignedUrl(branding.getLogoS3Key(), LOGO_URL_EXPIRY);
    }

    private BusinessResponse toBusinessResponse(Tenant tenant, TenantBranding branding) {
        String logoUrl = resolveLogoUrl(branding);

        BusinessResponse.BrandingInfo brandingInfo = new BusinessResponse.BrandingInfo(
            branding.getPrimaryColor(),
            branding.getBgColor(),
            branding.getFontFamily(),
            logoUrl,
            branding.isHidePoweredBy()
        );

        return new BusinessResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getPlan(),
            tenant.getAddress(),
            tenant.getPhone(),
            tenant.getWebsiteUrl(),
            brandingInfo,
            tenant.getCreatedAt(),
            tenant.getUpdatedAt()
        );
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}
