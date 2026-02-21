package com.waiveliability.modules.submissions.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waiveliability.common.exception.ApiException;
import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.common.storage.S3Service;
import com.waiveliability.modules.document.service.DocumentService;
import com.waiveliability.modules.forms.domain.Form;
import com.waiveliability.modules.forms.domain.FormField;
import com.waiveliability.modules.forms.repository.FormFieldRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.submissions.domain.Submission;
import com.waiveliability.modules.submissions.dto.SubmissionResponse;
import com.waiveliability.modules.submissions.dto.SubmitFormRequest;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final TenantRepository tenantRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final DocumentService documentService;

    public SubmissionResponse submitForm(String tenantSlug, UUID formId, SubmitFormRequest req) {
        // 1. Look up tenant by slug
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        // 2. Look up form — must be published and belong to tenant
        Form form = formRepository.findByIdAndTenantId(formId, tenant.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Form not found"));
        if (!"published".equals(form.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Form is not published");
        }

        // 3. Validate required fields
        List<FormField> fields = formFieldRepository.findByFormIdOrderByFieldOrder(formId);
        for (FormField field : fields) {
            if (field.isRequired()) {
                Object answer = req.answers() != null ? req.answers().get(field.getId().toString()) : null;
                if (answer == null || answer.toString().isBlank()) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Field '" + field.getLabel() + "' is required");
                }
            }
        }

        // 4. Extract submitter name and email from answers
        String submitterName = extractSubmitterName(fields, req.answers());
        String submitterEmail = extractSubmitterEmail(fields, req.answers());

        // 5. Store signature in S3 if provided
        String signatureKey = null;
        if (req.signatureData() != null && !req.signatureData().isBlank()) {
            signatureKey = uploadSignature(tenant.getId(), formId, req.signatureData());
        }

        // 6. Serialize form data
        String formDataJson = serializeAnswers(req.answers());

        // 7. Create submission
        Submission submission = Submission.builder()
            .form(form)
            .tenant(tenant)
            .submitterName(submitterName)
            .submitterEmail(submitterEmail)
            .formData(formDataJson)
            .signatureS3Key(signatureKey)
            .status("pending")
            .build();
        submissionRepository.save(submission);

        // 8. Trigger async PDF generation
        documentService.generatePdfAsync(submission.getId());

        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> getSubmissions(UUID tenantId, UUID formId, Pageable pageable) {
        Page<Submission> page = formId != null
            ? submissionRepository.findByFormIdAndTenantId(formId, tenantId, pageable)
            : submissionRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID tenantId, UUID id) {
        Submission s = submissionRepository.findById(id)
            .filter(sub -> sub.getTenant().getId().equals(tenantId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        return toResponse(s);
    }

    // --- helpers ---

    private String extractSubmitterName(List<FormField> fields, Map<String, Object> answers) {
        if (answers == null) return null;
        return fields.stream()
            .filter(f -> "text".equals(f.getFieldType()) &&
                f.getLabel().toLowerCase().contains("name"))
            .findFirst()
            .map(f -> {
                Object val = answers.get(f.getId().toString());
                return val != null ? val.toString() : null;
            })
            .orElse(null);
    }

    private String extractSubmitterEmail(List<FormField> fields, Map<String, Object> answers) {
        if (answers == null) return null;
        return fields.stream()
            .filter(f -> "email".equals(f.getFieldType()))
            .findFirst()
            .map(f -> {
                Object val = answers.get(f.getId().toString());
                return val != null ? val.toString() : null;
            })
            .orElse(null);
    }

    private String uploadSignature(UUID tenantId, UUID formId, String base64DataUri) {
        try {
            String base64 = base64DataUri.contains(",")
                ? base64DataUri.split(",")[1]
                : base64DataUri;
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            String key = String.format("signatures/%s/%s/%s.png", tenantId, formId, UUID.randomUUID());
            s3Service.upload(key, new ByteArrayInputStream(bytes), bytes.length, "image/png");
            return key;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid signature data");
        }
    }

    private String serializeAnswers(Map<String, Object> answers) {
        if (answers == null) return "{}";
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid answers format");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeAnswers(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private SubmissionResponse toResponse(Submission s) {
        String signatureUrl = null;
        if (s.getSignatureS3Key() != null) {
            try {
                signatureUrl = s3Service.generateSignedUrl(s.getSignatureS3Key(), Duration.ofSeconds(900));
            } catch (Exception ignored) {}
        }
        String pdfUrl = null;
        if (s.getPdfS3Key() != null) {
            try {
                pdfUrl = s3Service.generateSignedUrl(s.getPdfS3Key(), Duration.ofSeconds(900));
            } catch (Exception ignored) {}
        }
        return new SubmissionResponse(
            s.getId(),
            s.getForm().getId(),
            s.getSubmitterName(),
            s.getSubmitterEmail(),
            deserializeAnswers(s.getFormData()),
            signatureUrl,
            pdfUrl,
            s.getStatus(),
            s.getSubmittedAt()
        );
    }
}
