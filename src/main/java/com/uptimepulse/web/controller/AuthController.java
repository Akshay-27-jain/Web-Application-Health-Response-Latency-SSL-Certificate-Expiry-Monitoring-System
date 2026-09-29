package com.uptimepulse.web.controller;

import com.uptimepulse.application.service.AuthService;
import com.uptimepulse.web.dto.AuthResponse;
import com.uptimepulse.web.dto.LoginRequest;
import com.uptimepulse.web.dto.RegisterRequest;
import com.uptimepulse.web.security.RateLimitingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and login endpoints")
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;

    public AuthController(AuthService authService, RateLimitingService rateLimitingService) {
        this.authService = authService;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account with anti-bot verification")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        rateLimitingService.checkRateLimit("reg_" + clientIp);

        try {
            AuthResponse response = authService.register(request);
            rateLimitingService.recordSuccess("reg_" + clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            rateLimitingService.recordFailedAttempt("reg_" + clientIp);
            throw e;
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password with brute-force rate limiting")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String limitKey = "login_" + clientIp + "_" + request.getEmail().toLowerCase().trim();
        
        rateLimitingService.checkRateLimit(limitKey);

        try {
            AuthResponse response = authService.login(request);
            rateLimitingService.recordSuccess(limitKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            rateLimitingService.recordFailedAttempt(limitKey);
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
