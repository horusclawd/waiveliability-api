package com.waiveliability.modules.submissions.repository;

import com.waiveliability.modules.submissions.domain.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Page<Submission> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Submission> findByFormIdAndTenantId(UUID formId, UUID tenantId, Pageable pageable);
}
