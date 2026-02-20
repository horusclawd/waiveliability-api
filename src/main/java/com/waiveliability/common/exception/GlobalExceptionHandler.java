package com.waiveliability.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler producing RFC 7807 Problem Details responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://waiveliability.com/errors/";

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, WebRequest request) {
        log.warn("ApiException [{}]: {}", ex.getStatus(), ex.getMessage());
        var problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE_URI + ex.getStatus().value()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getAllErrors().stream()
            .filter(e -> e instanceof FieldError)
            .map(e -> (FieldError) e)
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation"));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
        problem.setType(URI.create(PROBLEM_BASE_URI + "401"));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create(PROBLEM_BASE_URI + "403"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create(PROBLEM_BASE_URI + "500"));
        return problem;
    }
}
