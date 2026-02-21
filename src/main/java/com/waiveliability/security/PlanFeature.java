package com.waiveliability.security;

/**
 * Enumeration of plan-gated features.
 * Used with @CheckPlanLimit AOP annotation (implemented Sprint 8).
 */
public enum PlanFeature {
    CUSTOM_BRANDING,
    CUSTOM_DOMAIN,
    SUBMISSION_EXPORT,
    ANALYTICS,
    TEAM_MEMBERS,
    SUBMISSION_ALERTS,
    UNLIMITED_FORMS,
    UNLIMITED_SUBMISSIONS,
    DOCUMENT_IMPORT
}
