package com.waiveliability.common.exception;

import org.springframework.http.HttpStatus;

public class PlanLimitExceededException extends ApiException {

    public PlanLimitExceededException(String feature) {
        super(HttpStatus.PAYMENT_REQUIRED, "Plan limit exceeded for feature: " + feature);
    }
}
