package com.waiveliability.modules.forms.controller;

import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.forms.dto.*;
import com.waiveliability.modules.forms.service.FormService;
import com.waiveliability.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

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
}
