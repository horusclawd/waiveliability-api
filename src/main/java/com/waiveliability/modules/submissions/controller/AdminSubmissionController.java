package com.waiveliability.modules.submissions.controller;

import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.submissions.dto.SubmissionResponse;
import com.waiveliability.modules.submissions.service.SubmissionService;
import com.waiveliability.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/submissions")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final SubmissionService submissionService;

    @GetMapping
    public PageResponse<SubmissionResponse> list(
        @RequestParam(required = false) UUID formId,
        @PageableDefault(size = 20) Pageable pageable) {
        return submissionService.getSubmissions(TenantContext.current(), formId, pageable);
    }

    @GetMapping("/{id}")
    public SubmissionResponse get(@PathVariable UUID id) {
        return submissionService.getSubmission(TenantContext.current(), id);
    }
}
