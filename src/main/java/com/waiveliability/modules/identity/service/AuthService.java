package com.waiveliability.modules.identity.service;

import com.waiveliability.common.exception.ApiException;
import com.waiveliability.config.JwtConfig;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.domain.User;
import com.waiveliability.modules.identity.dto.*;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.identity.repository.UserRepository;
import com.waiveliability.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "admin";
    private static final String DEFAULT_PLAN = "free";
    private static final String ACCESS_COOKIE  = "access_token";
    private static final String REFRESH_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletResponse response) {
        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        Tenant tenant = Tenant.builder()
            .name(req.businessName())
            .slug(generateSlug(req.businessName()))
            .plan(DEFAULT_PLAN)
            .build();
        tenantRepository.save(tenant);

        User user = User.builder()
            .tenant(tenant)
            .email(req.email().toLowerCase())
            .passwordHash(passwordEncoder.encode(req.password()))
            .name(req.name())
            .role(DEFAULT_ROLE)
            .build();
        userRepository.save(user);

        issueTokenCookies(user, response);
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        issueTokenCookies(user, response);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken, HttpServletResponse response) {
        Claims claims;
        try {
            claims = jwtService.validateAndParseClaims(refreshToken);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token type");
        }

        UUID userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        issueTokenCookies(user, response);
        return toAuthResponse(user);
    }

    public void logout(HttpServletResponse response) {
        clearCookie(response, ACCESS_COOKIE);
        clearCookie(response, REFRESH_COOKIE);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Tenant tenant = user.getTenant();
        return new MeResponse(
            user.getId(),
            tenant.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getPlan(),
            user.getCreatedAt()
        );
    }

    private void issueTokenCookies(User user, HttpServletResponse response) {
        String accessToken = jwtService.issueAccessToken(
            user.getId(), user.getTenant().getId(), user.getRole());
        String refreshToken = jwtService.issueRefreshToken(
            user.getId(), user.getTenant().getId(), user.getRole());

        addCookie(response, ACCESS_COOKIE, accessToken, (int) jwtConfig.getAccessTokenExpiry());
        addCookie(response, REFRESH_COOKIE, refreshToken, (int) jwtConfig.getRefreshTokenExpiry());
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
            user.getId(),
            user.getTenant().getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }

    private String generateSlug(String businessName) {
        String normalized = Normalizer.normalize(businessName, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\p{ASCII}]").matcher(normalized).replaceAll("")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");

        // Ensure uniqueness
        String base = slug.length() > 90 ? slug.substring(0, 90) : slug;
        String candidate = base;
        int suffix = 1;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
