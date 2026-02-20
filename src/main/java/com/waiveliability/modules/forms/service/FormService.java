package com.waiveliability.modules.forms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waiveliability.common.exception.ApiException;
import com.waiveliability.common.exception.PlanLimitExceededException;
import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.forms.domain.Form;
import com.waiveliability.modules.forms.domain.FormField;
import com.waiveliability.modules.forms.dto.*;
import com.waiveliability.modules.forms.repository.FormFieldRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FormService {

    private static final int FREE_FORM_LIMIT  = 3;
    private static final int BASIC_FORM_LIMIT = 10;

    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<FormSummaryResponse> getForms(UUID tenantId, Pageable pageable) {
        // TODO: N+1 on fields.size() — replace with a COUNT subquery or join fetch when performance matters
        Page<FormSummaryResponse> page = formRepository.findByTenantId(tenantId, pageable)
            .map(form -> new FormSummaryResponse(
                form.getId(),
                form.getName(),
                form.getDescription(),
                form.getStatus(),
                form.getFields().size(),
                form.getCreatedAt(),
                form.getUpdatedAt()
            ));
        return PageResponse.of(page);
    }

    public FormResponse createForm(UUID tenantId, CreateFormRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        int formCount = formRepository.countByTenantId(tenantId);
        int limit = switch (tenant.getPlan()) {
            case "free"  -> FREE_FORM_LIMIT;
            case "basic" -> BASIC_FORM_LIMIT;
            default      -> Integer.MAX_VALUE; // premium = unlimited
        };
        if (formCount >= limit) {
            throw new PlanLimitExceededException("forms");
        }

        Form form = Form.builder()
            .tenant(tenant)
            .name(req.name())
            .description(req.description())
            .status("draft")
            .build();
        formRepository.save(form);

        return toFormResponse(form);
    }

    @Transactional(readOnly = true)
    public FormResponse getForm(UUID tenantId, UUID formId) {
        Form form = formRepository.findByIdAndTenantId(formId, tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));
        return toFormResponse(form);
    }

    public FormResponse updateForm(UUID tenantId, UUID formId, UpdateFormRequest req) {
        Form form = formRepository.findByIdAndTenantId(formId, tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));

        form.setName(req.name());
        form.setDescription(req.description());

        // Full replace of fields — delete all existing, then recreate from request
        formFieldRepository.deleteByFormId(formId);
        formFieldRepository.flush();

        List<FormField> newFields = new ArrayList<>();
        for (FieldRequest fieldReq : req.fields()) {
            FormField field = FormField.builder()
                .form(form)
                .fieldType(fieldReq.fieldType().toLowerCase())
                .label(fieldReq.label())
                .placeholder(fieldReq.placeholder())
                .required(fieldReq.required())
                .fieldOrder(fieldReq.fieldOrder())
                .options(serializeOptions(fieldReq.options()))
                .build();
            newFields.add(field);
        }
        formFieldRepository.saveAll(newFields);
        formRepository.save(form);
        return toFormResponse(form);
    }

    public void deleteForm(UUID tenantId, UUID formId) {
        Form form = formRepository.findByIdAndTenantId(formId, tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));
        formRepository.delete(form);
    }

    public FormResponse publishForm(UUID tenantId, UUID formId) {
        Form form = formRepository.findByIdAndTenantId(formId, tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));
        form.setStatus("published");
        formRepository.save(form);
        return toFormResponse(form);
    }

    public FormResponse unpublishForm(UUID tenantId, UUID formId) {
        Form form = formRepository.findByIdAndTenantId(formId, tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));
        form.setStatus("draft");
        formRepository.save(form);
        return toFormResponse(form);
    }

    public FormResponse duplicateForm(UUID tenantId, UUID formId) {
        Form original = formRepository.findByIdAndTenantId(formId, tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Form copy = Form.builder()
            .tenant(tenant)
            .name("Copy of " + original.getName())
            .description(original.getDescription())
            .status("draft")
            .build();
        formRepository.save(copy);

        List<FormField> originalFields = formFieldRepository.findByFormIdOrderByFieldOrder(formId);
        List<FormField> copiedFields = originalFields.stream().map(f ->
            FormField.builder()
                .form(copy)
                .fieldType(f.getFieldType())
                .label(f.getLabel())
                .placeholder(f.getPlaceholder())
                .required(f.isRequired())
                .fieldOrder(f.getFieldOrder())
                .options(f.getOptions())
                .build()
        ).toList();
        formFieldRepository.saveAll(copiedFields);

        return toFormResponse(copy);
    }

    // --- helpers ---

    private FormResponse toFormResponse(Form form) {
        List<FormField> dbFields = formFieldRepository.findByFormIdOrderByFieldOrder(form.getId());
        List<FormFieldResponse> fieldResponses = dbFields.stream()
            .map(this::toFieldResponse)
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

    private FormFieldResponse toFieldResponse(FormField field) {
        List<FormFieldOption> options = deserializeOptions(field.getOptions());
        return new FormFieldResponse(
            field.getId(),
            field.getFieldType(),
            field.getLabel(),
            field.getPlaceholder(),
            field.isRequired(),
            field.getFieldOrder(),
            options
        );
    }

    private String serializeOptions(List<FormFieldOption> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid options format");
        }
    }

    private List<FormFieldOption> deserializeOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FormFieldOption>>() {});
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse field options");
        }
    }
}
