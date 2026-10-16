package com.company.employeeleave.controller;

import com.company.employeeleave.dto.AuthRequest;
import com.company.employeeleave.dto.AuthResponse;
import com.company.employeeleave.dto.RegisterEmployeeRequest;
import com.company.employeeleave.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and public employee registration endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password", description = "Returns a signed JWT bearer token upon successful authentication.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new employee", description = "Public self-registration strictly assigning ROLE_EMPLOYEE and safe defaults.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterEmployeeRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Returns active user identity and role from token.")
    public ResponseEntity<AuthResponse> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication));
    }
}
