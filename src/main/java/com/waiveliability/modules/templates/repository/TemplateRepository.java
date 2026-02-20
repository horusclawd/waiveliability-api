package com.waiveliability.modules.templates.repository;

import com.waiveliability.modules.templates.domain.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    Page<Template> findAll(Pageable pageable);

    Page<Template> findByCategory(String category, Pageable pageable);

    @Modifying
    @Query("UPDATE Template t SET t.usageCount = t.usageCount + 1 WHERE t.id = :id")
    void incrementUsageCount(@Param("id") UUID id);
}
