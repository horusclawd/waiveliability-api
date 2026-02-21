package com.waiveliability.modules.submissions.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waiveliability.common.exception.ApiException;
import com.waiveliability.common.exception.PlanLimitExceededException;
import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.common.storage.S3Service;
import com.waiveliability.modules.document.service.DocumentService;
import com.waiveliability.modules.forms.domain.Form;
import com.waiveliability.modules.forms.domain.FormField;
import com.waiveliability.modules.forms.repository.FormFieldRepository;
import com.waiveliability.modules.forms.repository.FormRepository;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.notification.service.EmailService;
import com.waiveliability.modules.submissions.domain.Submission;
import com.waiveliability.modules.submissions.dto.SubmissionResponse;
import com.waiveliability.modules.submissions.dto.SubmitFormRequest;
import com.waiveliability.modules.submissions.repository.SubmissionRepository;
import com.waiveliability.modules.submissions.repository.SubmissionSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubmissionService {

    private static final Set<String> VALID_STATUSES = Set.of("pending", "reviewed", "archived");

    private final SubmissionRepository submissionRepository;
    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final TenantRepository tenantRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final DocumentService documentService;
    private final EmailService emailService;

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

        // 9. Send confirmation email to submitter (async)
        emailService.sendSubmissionConfirmation(submission, tenant);

        // 10. Send alert to tenant admin if on Basic+ plan (async)
        var plan = tenant.getPlan() != null
            ? com.waiveliability.modules.billing.domain.Subscription.PlanType.valueOf(tenant.getPlan().toLowerCase())
            : com.waiveliability.modules.billing.domain.Subscription.PlanType.free;
        emailService.sendNewSubmissionAlert(submission, tenant, plan);

        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubmissionResponse> getSubmissions(UUID tenantId, UUID formId, String status,
                                                            String submitterName, Instant startDate,
                                                            Instant endDate, Pageable pageable) {
        Specification<Submission> spec = buildSpecification(tenantId, formId, status, submitterName, startDate, endDate);
        Page<Submission> page = submissionRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID tenantId, UUID id) {
        Submission s = submissionRepository.findById(id)
            .filter(sub -> sub.getTenant().getId().equals(tenantId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getPublicSubmission(String tenantSlug, UUID submissionId) {
        // Look up tenant by slug
        var tenant = tenantRepository.findBySlug(tenantSlug)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        // Look up submission by ID and tenant
        Submission s = submissionRepository.findById(submissionId)
            .filter(sub -> sub.getTenant().getId().equals(tenant.getId()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        return toResponse(s);
    }

    public SubmissionResponse updateStatus(UUID tenantId, UUID id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid status. Must be one of: " + VALID_STATUSES);
        }

        Submission submission = submissionRepository.findById(id)
            .filter(sub -> sub.getTenant().getId().equals(tenantId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));

        submission.setStatus(status);
        submissionRepository.save(submission);
        return toResponse(submission);
    }

    public void deleteSubmission(UUID tenantId, UUID id) {
        Submission submission = submissionRepository.findById(id)
            .filter(sub -> sub.getTenant().getId().equals(tenantId))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));

        // Clean up S3 objects — don't fail the delete if cleanup fails
        if (submission.getSignatureS3Key() != null) {
            try {
                s3Service.delete(submission.getSignatureS3Key());
            } catch (Exception e) {
                log.error("Failed to delete signature S3 object: {}", submission.getSignatureS3Key(), e);
            }
        }
        if (submission.getPdfS3Key() != null) {
            try {
                s3Service.delete(submission.getPdfS3Key());
            } catch (Exception e) {
                log.error("Failed to delete PDF S3 object: {}", submission.getPdfS3Key(), e);
            }
        }

        submissionRepository.delete(submission);
    }

    @Transactional(readOnly = true)
    public void exportCsv(UUID tenantId, UUID formId, String status, String submitterName,
                           Instant startDate, Instant endDate, PrintWriter writer) {
        // Plan gate: only premium tenants can export
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));
        String plan = tenant.getPlan() != null ? tenant.getPlan() : "free";
        if (!"premium".equals(plan)) {
            throw new PlanLimitExceededException("csv_export");
        }

        Specification<Submission> spec = buildSpecification(tenantId, formId, status, submitterName, startDate, endDate);
        // Limit to prevent memory exhaustion - export largest reasonable dataset
        org.springframework.data.domain.Pageable exportPageable =
            org.springframework.data.domain.PageRequest.of(0, 10000);
        List<Submission> submissions = submissionRepository.findAll(spec, exportPageable).getContent();

        // Write CSV header
        writer.println("id,form_id,submitter_name,submitter_email,status,submitted_at");

        // Write CSV rows
        for (Submission s : submissions) {
            writer.println(String.join(",",
                escapeCsv(s.getId().toString()),
                escapeCsv(s.getForm().getId().toString()),
                escapeCsv(s.getSubmitterName()),
                escapeCsv(s.getSubmitterEmail()),
                escapeCsv(s.getStatus()),
                escapeCsv(s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "")
            ));
        }

        writer.flush();
    }

    // --- helpers ---

    private Specification<Submission> buildSpecification(UUID tenantId, UUID formId, String status,
                                                          String submitterName, Instant startDate, Instant endDate) {
        Specification<Submission> spec = Specification.where(SubmissionSpecifications.hasTenantId(tenantId));

        if (formId != null) {
            spec = spec.and(SubmissionSpecifications.hasFormId(formId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and(SubmissionSpecifications.hasStatus(status));
        }
        if (submitterName != null && !submitterName.isBlank()) {
            spec = spec.and(SubmissionSpecifications.submitterNameContains(submitterName));
        }
        if (startDate != null || endDate != null) {
            spec = spec.and(SubmissionSpecifications.submittedBetween(startDate, endDate));
        }

        return spec;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

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
