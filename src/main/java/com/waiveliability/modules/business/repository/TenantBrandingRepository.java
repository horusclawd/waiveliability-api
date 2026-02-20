package com.waiveliability.modules.business.repository;

import com.waiveliability.modules.business.domain.TenantBranding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantBrandingRepository extends JpaRepository<TenantBranding, UUID> {
}
