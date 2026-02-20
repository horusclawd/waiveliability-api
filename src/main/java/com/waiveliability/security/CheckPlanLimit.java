package com.waiveliability.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as plan-gated.
 * The PlanEnforcer AOP aspect checks the current tenant's plan before allowing execution.
 *
 * Usage:
 *   @CheckPlanLimit(feature = PlanFeature.CUSTOM_BRANDING)
 *   public void updateBranding(BrandingRequest req) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPlanLimit {
    PlanFeature feature();
}
