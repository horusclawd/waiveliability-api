package com.waiveliability.modules.forms.repository;

import com.waiveliability.modules.forms.domain.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FormRepository extends JpaRepository<Form, UUID> {

    Page<Form> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Form> findByIdAndTenantId(UUID id, UUID tenantId);

    int countByTenantId(UUID tenantId);
}
