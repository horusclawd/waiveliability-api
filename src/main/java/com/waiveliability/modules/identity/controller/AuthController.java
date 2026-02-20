package com.waiveliability.modules.identity.controller;

import com.waiveliability.common.exception.ApiException;
import com.waiveliability.modules.identity.dto.AuthResponse;
import com.waiveliability.modules.identity.dto.LoginRequest;
import com.waiveliability.modules.identity.dto.RegisterRequest;
import com.waiveliability.modules.identity.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletResponse response
    ) {
        AuthResponse body = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.login(request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken = extractRefreshCookie(request);
        return ResponseEntity.ok(authService.refresh(refreshToken, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "No refresh token");
        }
        return Arrays.stream(request.getCookies())
            .filter(c -> REFRESH_COOKIE.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No refresh token"));
    }
}
