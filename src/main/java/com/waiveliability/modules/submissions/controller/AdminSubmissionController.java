package com.waiveliability.modules.submissions.controller;

import com.waiveliability.common.pagination.PageResponse;
import com.waiveliability.modules.submissions.dto.SubmissionResponse;
import com.waiveliability.modules.submissions.dto.UpdateSubmissionStatusRequest;
import com.waiveliability.modules.submissions.service.SubmissionService;
import com.waiveliability.security.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/submissions")
@RequiredArgsConstructor
public class AdminSubmissionController {

    private final SubmissionService submissionService;

    @GetMapping
    public PageResponse<SubmissionResponse> list(
        @RequestParam(required = false) UUID formId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String submitterName,
        @RequestParam(required = false) Instant startDate,
        @RequestParam(required = false) Instant endDate,
        @PageableDefault(size = 20) Pageable pageable) {
        return submissionService.getSubmissions(
            TenantContext.current(), formId, status, submitterName, startDate, endDate, pageable);
    }

    @GetMapping("/export")
    public void exportCsv(
        @RequestParam(required = false) UUID formId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String submitterName,
        @RequestParam(required = false) Instant startDate,
        @RequestParam(required = false) Instant endDate,
        HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"submissions.csv\"");

        PrintWriter writer = response.getWriter();
        submissionService.exportCsv(
            TenantContext.current(), formId, status, submitterName, startDate, endDate, writer);
    }

    @GetMapping("/{id}")
    public SubmissionResponse get(@PathVariable UUID id) {
        return submissionService.getSubmission(TenantContext.current(), id);
    }

    @PatchMapping("/{id}/status")
    public SubmissionResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateSubmissionStatusRequest request) {
        return submissionService.updateStatus(TenantContext.current(), id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        submissionService.deleteSubmission(TenantContext.current(), id);
    }
}
