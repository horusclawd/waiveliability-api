package com.waiveliability.modules.templates.controller;

import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.forms.dto.FormResponse;
import com.waiveliability.modules.templates.dto.TemplateSummaryResponse;
import com.waiveliability.modules.templates.dto.TemplateResponse;
import com.waiveliability.modules.templates.service.TemplateService;
import com.waiveliability.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public PageResponse<TemplateSummaryResponse> list(
        @RequestParam(required = false) String category,
        Pageable pageable) {
        return templateService.getTemplates(category, pageable);
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@PathVariable UUID id) {
        return templateService.getTemplate(id);
    }

    @PostMapping("/{id}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public FormResponse importTemplate(@PathVariable UUID id) {
        return templateService.importTemplate(TenantContext.current(), id);
    }
}
