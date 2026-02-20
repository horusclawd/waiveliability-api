package com.waiveliability.modules.identity.service;

import com.waiveliability.common.exception.ApiException;
import com.waiveliability.config.JwtConfig;
import com.waiveliability.modules.identity.domain.Tenant;
import com.waiveliability.modules.identity.domain.User;
import com.waiveliability.modules.identity.dto.LoginRequest;
import com.waiveliability.modules.identity.dto.RegisterRequest;
import com.waiveliability.modules.identity.repository.TenantRepository;
import com.waiveliability.modules.identity.repository.UserRepository;
import com.waiveliability.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private JwtService jwtService;
    @Mock private HttpServletResponse httpResponse;

    private PasswordEncoder passwordEncoder;
    private JwtConfig jwtConfig;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtConfig = new JwtConfig();
        jwtConfig.setAccessTokenExpiry(900L);
        jwtConfig.setRefreshTokenExpiry(604800L);
        authService = new AuthService(
            userRepository, tenantRepository, passwordEncoder, jwtService, jwtConfig);
    }

    @Test
    void register_createsUserAndTenantAtomically() {
        RegisterRequest req = new RegisterRequest("Jane Doe", "jane@example.com", "password123", "Acme Inc");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(tenantRepository.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.issueAccessToken(any(), any(), anyString())).thenReturn("access-token");
        when(jwtService.issueRefreshToken(any(), any(), anyString())).thenReturn("refresh-token");

        var result = authService.register(req, httpResponse);

        assertThat(result.email()).isEqualTo("jane@example.com");
        assertThat(result.name()).isEqualTo("Jane Doe");
        assertThat(result.role()).isEqualTo("admin");

        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflict_whenEmailExists() {
        RegisterRequest req = new RegisterRequest("Jane", "jane@example.com", "password123", "Acme");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req, httpResponse))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void login_returnsAuthResponse_withValidCredentials() {
        String rawPassword = "password123";
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("Acme").slug("acme").plan("free").build();
        User user = User.builder()
            .id(UUID.randomUUID())
            .tenant(tenant)
            .email("jane@example.com")
            .passwordHash(passwordEncoder.encode(rawPassword))
            .name("Jane Doe")
            .role("admin")
            .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.issueAccessToken(any(), any(), anyString())).thenReturn("access-token");
        when(jwtService.issueRefreshToken(any(), any(), anyString())).thenReturn("refresh-token");

        var result = authService.login(new LoginRequest("jane@example.com", rawPassword), httpResponse);

        assertThat(result.email()).isEqualTo("jane@example.com");
    }

    @Test
    void login_throwsUnauthorized_whenPasswordWrong() {
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("Acme").slug("acme").plan("free").build();
        User user = User.builder()
            .id(UUID.randomUUID())
            .tenant(tenant)
            .email("jane@example.com")
            .passwordHash(passwordEncoder.encode("correct-password"))
            .name("Jane")
            .role("admin")
            .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong"), httpResponse))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_throwsUnauthorized_whenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "pass"), httpResponse))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void logout_clearsCookies() {
        authService.logout(httpResponse);
        verify(httpResponse, times(2)).addCookie(any());
    }
}
