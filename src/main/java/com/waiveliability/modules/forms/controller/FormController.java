package com.waiveliability.modules.forms.controller;

import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.forms.dto.*;
import com.waiveliability.modules.forms.service.DocumentImportService;
import com.waiveliability.modules.forms.service.FormService;
import com.waiveliability.security.CheckPlanLimit;
import com.waiveliability.security.PlanFeature;
import com.waiveliability.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;
    private final DocumentImportService documentImportService;

    @GetMapping
    public ResponseEntity<PageResponse<FormSummaryResponse>> getForms(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(formService.getForms(TenantContext.current(), pageable));
    }

    @PostMapping
    public ResponseEntity<FormResponse> createForm(
        @Valid @RequestBody CreateFormRequest request
    ) {
        FormResponse response = formService.createForm(TenantContext.current(), request);
        return ResponseEntity
            .created(URI.create("/api/v1/admin/forms/" + response.id()))
            .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormResponse> getForm(@PathVariable UUID id) {
        return ResponseEntity.ok(formService.getForm(TenantContext.current(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormResponse> updateForm(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateFormRequest request
    ) {
        return ResponseEntity.ok(formService.updateForm(TenantContext.current(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@PathVariable UUID id) {
        formService.deleteForm(TenantContext.current(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<FormResponse> publishForm(@PathVariable UUID id) {
        return ResponseEntity.ok(formService.publishForm(TenantContext.current(), id));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<FormResponse> unpublishForm(@PathVariable UUID id) {
        return ResponseEntity.ok(formService.unpublishForm(TenantContext.current(), id));
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public FormResponse duplicateForm(@PathVariable UUID id) {
        return formService.duplicateForm(TenantContext.current(), id);
    }

    @PostMapping("/import")
    @CheckPlanLimit(feature = PlanFeature.DOCUMENT_IMPORT)
    public ResponseEntity<ImportPreviewResponse> importDocument(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var fields = documentImportService.parseDocument(file);
        String extractedText = fields.stream()
                .map(DetectedField::getContent)
                .filter(c -> c != null)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        return ResponseEntity.ok(new ImportPreviewResponse(
                file.getOriginalFilename(),
                extractedText,
                fields
        ));
    }
}
