package com.waiveliability.security;

import com.waiveliability.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final String role = "admin";

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-must-be-at-least-256-bits-long!!");
        config.setAccessTokenExpiry(900L);
        config.setRefreshTokenExpiry(604800L);
        jwtService = new JwtService(config);
    }

    @Test
    void issueAccessToken_parsesCorrectly() {
        String token = jwtService.issueAccessToken(userId, tenantId, role);

        Claims claims = jwtService.validateAndParseClaims(token);

        assertThat(jwtService.isAccessToken(claims)).isTrue();
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractTenantId(claims)).isEqualTo(tenantId);
        assertThat(jwtService.extractRole(claims)).isEqualTo(role);
    }

    @Test
    void issueRefreshToken_parsesCorrectly() {
        String token = jwtService.issueRefreshToken(userId, tenantId, role);

        Claims claims = jwtService.validateAndParseClaims(token);

        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.isAccessToken(claims)).isFalse();
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
    }

    @Test
    void isTokenValid_returnsFalseForGarbage() {
        assertThat(jwtService.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForWrongSecret() {
        JwtConfig otherConfig = new JwtConfig();
        otherConfig.setSecret("completely-different-secret-key-must-be-256-bits-long!!");
        otherConfig.setAccessTokenExpiry(900L);
        otherConfig.setRefreshTokenExpiry(604800L);
        JwtService other = new JwtService(otherConfig);

        String token = other.issueAccessToken(userId, tenantId, role);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void validateAndParseClaims_throwsOnExpiredToken() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-must-be-at-least-256-bits-long!!");
        config.setAccessTokenExpiry(-1L); // already expired
        config.setRefreshTokenExpiry(-1L);
        JwtService expiredService = new JwtService(config);

        String token = expiredService.issueAccessToken(userId, tenantId, role);

        assertThatThrownBy(() -> jwtService.validateAndParseClaims(token))
            .isInstanceOf(Exception.class);
    }
}
