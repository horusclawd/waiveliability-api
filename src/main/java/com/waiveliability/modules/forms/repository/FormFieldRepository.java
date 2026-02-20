package com.waiveliability.modules.forms.repository;

import com.waiveliability.modules.forms.domain.FormField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormFieldRepository extends JpaRepository<FormField, UUID> {

    List<FormField> findByFormIdOrderByFieldOrder(UUID formId);

    void deleteByFormId(UUID formId);
}
