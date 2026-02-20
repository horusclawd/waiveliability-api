package com.waiveliability.security;

import com.waiveliability.common.exception.PlanLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces plan limits declaratively via @CheckPlanLimit.
 * Full plan logic wired in Sprint 8 when billing module is complete.
 */
@Aspect
@Component
@Slf4j
public class PlanEnforcer {

    @Before("@annotation(checkPlanLimit)")
    public void enforce(JoinPoint joinPoint, CheckPlanLimit checkPlanLimit) {
        PlanFeature feature = checkPlanLimit.feature();
        log.debug("Checking plan limit for feature: {}, tenant: {}", feature, TenantContext.current());

        // Sprint 8: query tenant's plan and enforce limits
        // For now, allow all — enforcement logic added when billing is implemented
    }
}
