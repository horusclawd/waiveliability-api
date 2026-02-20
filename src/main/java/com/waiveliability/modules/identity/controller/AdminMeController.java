package com.waiveliability.modules.identity.controller;

import com.waiveliability.modules.identity.dto.MeResponse;
import com.waiveliability.modules.identity.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMeController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.getMe(UUID.fromString(userId)));
    }
}
