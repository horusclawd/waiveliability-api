package com.waiveliability.modules.submissions.controller;

import com.waiveliability.modules.submissions.dto.SubmissionResponse;
import com.waiveliability.modules.submissions.dto.SubmitFormRequest;
import com.waiveliability.modules.submissions.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/{tenantSlug}")
@RequiredArgsConstructor
public class PublicSubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/forms/{formId}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submitForm(
        @PathVariable String tenantSlug,
        @PathVariable UUID formId,
        @RequestBody SubmitFormRequest req) {
        return submissionService.submitForm(tenantSlug, formId, req);
    }
}
