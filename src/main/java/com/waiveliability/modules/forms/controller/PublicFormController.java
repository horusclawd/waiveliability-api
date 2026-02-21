package com.waiveliability.modules.forms.controller;

import com.waiveliability.modules.forms.dto.FormResponse;
import com.waiveliability.modules.forms.service.FormService;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/{tenantSlug}/forms")
@RequiredArgsConstructor
public class PublicFormController {

    private final FormService formService;
    private final TenantRepository tenantRepository;

    @GetMapping("/{formId}")
    public FormResponse getPublicForm(
        @PathVariable String tenantSlug,
        @PathVariable UUID formId
    ) {
        // Look up tenant by slug
        var tenant = tenantRepository.findBySlug(tenantSlug)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        // Get form - must be published and belong to tenant
        return formService.getPublicForm(tenant.getId(), formId);
    }
}
