package com.waiveliability.modules.templates.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waiveliability.common.exception.ApiException;
import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.forms.domain.Form;
import com.waiveliability.modules.forms.domain.FormField;
import com.waiveliability.modules.forms.dto.FormFieldOption;
import com.waiveliability.modules.forms.dto.FormFieldResponse;
import com.waiveliability.modules.forms.dto.FormResponse;
import com.waiveliability.modules.forms.repository.FormFieldRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.templates.domain.Template;
import com.waiveliability.modules.templates.domain.TemplateField;
import com.waiveliability.modules.templates.dto.TemplateFieldResponse;
import com.waiveliability.modules.templates.dto.TemplateResponse;
import com.waiveliability.modules.templates.dto.TemplateSummaryResponse;
import com.waiveliability.modules.templates.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<TemplateSummaryResponse> getTemplates(String category, Pageable pageable) {
        Page<Template> page = (category != null && !category.isBlank())
            ? templateRepository.findByCategory(category, pageable)
            : templateRepository.findAll(pageable);
        return PageResponse.of(page.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(UUID id) {
        Template t = templateRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        return toResponse(t);
    }

    public FormResponse importTemplate(UUID tenantId, UUID templateId) {
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Form form = Form.builder()
            .tenant(tenant)
            .name(template.getName())
            .description(template.getDescription())
            .status("draft")
            .build();
        formRepository.save(form);

        List<FormField> fields = template.getFields().stream().map(tf ->
            FormField.builder()
                .form(form)
                .fieldType(tf.getFieldType())
                .label(tf.getLabel())
                .placeholder(tf.getPlaceholder())
                .required(tf.isRequired())
                .fieldOrder(tf.getFieldOrder())
                .options(tf.getOptions())
                .content(tf.getContent())
                .build()
        ).toList();
        formFieldRepository.saveAll(fields);

        templateRepository.incrementUsageCount(templateId);

        return toFormResponse(form, fields);
    }

    // --- helpers ---

    private TemplateSummaryResponse toSummary(Template t) {
        return new TemplateSummaryResponse(
            t.getId(),
            t.getName(),
            t.getDescription(),
            t.getCategory(),
            t.isPremium(),
            t.getUsageCount(),
            t.getFields().size(),
            t.getCreatedAt(),
            t.getUpdatedAt()
        );
    }

    private TemplateResponse toResponse(Template t) {
        List<TemplateFieldResponse> fieldResponses = t.getFields().stream()
            .map(this::toFieldResponse)
            .toList();
        return new TemplateResponse(
            t.getId(),
            t.getName(),
            t.getDescription(),
            t.getCategory(),
            t.isPremium(),
            t.getUsageCount(),
            fieldResponses,
            t.getCreatedAt(),
            t.getUpdatedAt()
        );
    }

    private TemplateFieldResponse toFieldResponse(TemplateField tf) {
        List<FormFieldOption> options = deserializeOptions(tf.getOptions());
        return new TemplateFieldResponse(
            tf.getId(),
            tf.getFieldType(),
            tf.getLabel(),
            tf.getPlaceholder(),
            tf.isRequired(),
            tf.getFieldOrder(),
            options
        );
    }

    private FormResponse toFormResponse(Form form, List<FormField> fields) {
        List<FormFieldResponse> fieldResponses = fields.stream()
            .map(f -> new FormFieldResponse(
                f.getId(),
                f.getFieldType(),
                f.getLabel(),
                f.getPlaceholder(),
                f.isRequired(),
                f.getFieldOrder(),
                deserializeOptions(f.getOptions()),
                f.getContent()
            ))
            .toList();
        return new FormResponse(
            form.getId(),
            form.getName(),
            form.getDescription(),
            form.getStatus(),
            fieldResponses,
            form.getCreatedAt(),
            form.getUpdatedAt()
        );
    }

    private List<FormFieldOption> deserializeOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FormFieldOption>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
