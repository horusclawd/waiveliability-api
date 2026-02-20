package com.waiveliability.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the Bearer JWT on every request and populates TenantContext.
 * Full implementation in Sprint 1 when JJWT service is wired up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        try {
            // Sprint 1: extract + validate JWT, populate SecurityContext + TenantContext
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
