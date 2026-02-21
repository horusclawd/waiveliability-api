package com.waiveliability.modules.submissions.controller;

import com.waiveliability.common.exception.ApiException;
import com.waiveliability.common.ratelimit.RateLimiterService;
import com.waiveliability.modules.submissions.dto.SubmissionResponse;
import com.waiveliability.modules.submissions.dto.SubmitFormRequest;
import com.waiveliability.modules.submissions.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/{tenantSlug}")
@RequiredArgsConstructor
public class PublicSubmissionController {

    private final SubmissionService submissionService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/forms/{formId}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submitForm(
        @PathVariable String tenantSlug,
        @PathVariable UUID formId,
        @RequestBody SubmitFormRequest req,
        HttpServletRequest request) {
        // Rate limit by IP address
        String clientIp = getClientIp(request);
        rateLimiterService.checkRateLimit(clientIp);

        return submissionService.submitForm(tenantSlug, formId, req);
    }

    @GetMapping("/submissions/{submissionId}")
    public SubmissionResponse getSubmission(
        @PathVariable String tenantSlug,
        @PathVariable UUID submissionId) {
        return submissionService.getPublicSubmission(tenantSlug, submissionId);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
