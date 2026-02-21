package com.waiveliability.modules.submissions.repository;

import com.waiveliability.modules.submissions.domain.Submission;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class SubmissionSpecifications {

    private SubmissionSpecifications() {}

    public static Specification<Submission> hasTenantId(UUID tenantId) {
        return (root, query, cb) -> {
            if (tenantId == null) return null;
            return cb.equal(root.get("tenant").get("id"), tenantId);
        };
    }

    public static Specification<Submission> hasFormId(UUID formId) {
        return (root, query, cb) -> {
            if (formId == null) return null;
            return cb.equal(root.get("form").get("id"), formId);
        };
    }

    public static Specification<Submission> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Submission> submitterNameContains(String name) {
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("submitterName")),
            "%" + name.toLowerCase() + "%"
        );
    }

    public static Specification<Submission> submittedBetween(Instant start, Instant end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get("submittedAt"), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("submittedAt"), start);
            } else {
                return cb.lessThanOrEqualTo(root.get("submittedAt"), end);
            }
        };
    }
}
