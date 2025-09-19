package com.sparrow.auth_service.controller;

import com.sparrow.auth_service.dto.AuthResponse;
import com.sparrow.auth_service.dto.LoginRequest;
import com.sparrow.auth_service.dto.UserRegistrationRequest;
import com.sparrow.auth_service.dto.UserResponse;
import com.sparrow.auth_service.service.AuthService;
import com.sparrow.auth_service.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final KeycloakService keycloakService;

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.info("Login attempt for user: {}", request.getUsername());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            logger.info("Registration attempt for user: {}", request.getUsername());
            UserResponse response = keycloakService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            logger.info("Token refresh attempt");
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token refresh failed: " + e.getMessage());
        }
    }

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Get users by role")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            logger.info("Get users by role: {}", role);
            List<UserResponse> users = keycloakService.getUsersByRole(role);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to get users by role: {}", role, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to get users: " + e.getMessage());
        }
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        try {
            logger.info("Get user by ID: {}", userId);
            UserResponse user = keycloakService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Failed to get user by ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + e.getMessage());
        }
    }
}