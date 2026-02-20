package com.waiveliability.modules.submissions.dto;

import java.util.Map;

public record SubmitFormRequest(
    Map<String, Object> answers,
    String signatureData
) {}
