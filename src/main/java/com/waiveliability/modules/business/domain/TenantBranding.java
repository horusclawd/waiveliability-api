package com.waiveliability.modules.business.domain;

import com.waiveliability.modules.identity.domain.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tenant_branding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantBranding {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "bg_color", length = 7)
    private String bgColor;

    @Column(name = "font_family", length = 100)
    private String fontFamily;

    @Column(name = "logo_s3_key", length = 500)
    private String logoS3Key;

    @Column(name = "hide_powered_by", nullable = false)
    @Builder.Default
    private boolean hidePoweredBy = false;

    // custom_domain and domain_verified columns exist in the DB (V4 migration)
    // and will be mapped when the custom domain feature is implemented (Sprint 12).
}
