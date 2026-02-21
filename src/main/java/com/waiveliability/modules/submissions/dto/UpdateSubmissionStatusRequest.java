package com.waiveliability.modules.submissions.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSubmissionStatusRequest(
    @NotBlank String status
) {}
