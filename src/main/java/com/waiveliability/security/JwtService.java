package com.waiveliability.security;

import com.waiveliability.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final String CLAIM_ROLE = "role";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private final JwtConfig jwtConfig;

    public String issueAccessToken(UUID userId, UUID tenantId, String role) {
        return buildToken(userId, tenantId, role, TOKEN_TYPE_ACCESS, jwtConfig.getAccessTokenExpiry());
    }

    public String issueRefreshToken(UUID userId, UUID tenantId, String role) {
        return buildToken(userId, tenantId, role, TOKEN_TYPE_REFRESH, jwtConfig.getRefreshTokenExpiry());
    }

    private String buildToken(UUID userId, UUID tenantId, String role, String type, long expirySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_TENANT_ID, tenantId.toString())
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TOKEN_TYPE, type)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirySeconds)))
            .signWith(signingKey())
            .compact();
    }

    public Claims validateAndParseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractTenantId(Claims claims) {
        return UUID.fromString(claims.get(CLAIM_TENANT_ID, String.class));
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndParseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
