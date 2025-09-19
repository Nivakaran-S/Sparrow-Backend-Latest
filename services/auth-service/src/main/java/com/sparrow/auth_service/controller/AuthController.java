package com.sparrow.auth_service.controller;

import com.sparrow.auth_service.dto.AuthResponse;
import com.sparrow.auth_service.dto.LoginRequest;
import com.sparrow.auth_service.dto.UserRegistrationRequest;
import com.sparrow.auth_service.dto.UserResponse;
import com.sparrow.auth_service.dto.UserUpdateRequest;
import com.sparrow.auth_service.dto.PasswordChangeRequest;
import com.sparrow.auth_service.dto.RoleAssignmentRequest;
import com.sparrow.auth_service.service.AuthService;
import com.sparrow.auth_service.service.KeycloakService;
import com.sparrow.auth_service.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & User Management", description = "Authentication and User Management APIs")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final KeycloakService keycloakService;
    private final UserManagementService userManagementService;

    // ========================= AUTHENTICATION ENDPOINTS =========================

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.info("Login attempt for user: {}", request.getUsername());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user with default CUSTOMER role")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            logger.info("Registration attempt for user: {}", request.getUsername());

            // Set default role to CUSTOMER for public registration
            if (request.getRoles() == null || request.getRoles().isEmpty()) {
                request.setRoles(List.of("CUSTOMER"));
            }

            UserResponse response = keycloakService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Refresh JWT access token using refresh token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            logger.info("Token refresh attempt");
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate tokens")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> logout(@RequestParam String refreshToken, Principal principal) {
        try {
            logger.info("Logout attempt for user: {}", principal.getName());
            authService.logout(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }

    // ========================= PROFILE MANAGEMENT =========================

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Get authenticated user's profile information")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserProfile(Principal principal) {
        try {
            String username = principal.getName();
            UserResponse user = userManagementService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Failed to get profile for user: {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Profile not found", "message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile", description = "Update authenticated user's profile information")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCurrentUserProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Principal principal) {
        try {
            String username = principal.getName();
            UserResponse user = userManagementService.getUserByUsername(username);
            UserResponse updatedUser = userManagementService.updateUser(user.getId(), request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Failed to update profile for user: {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Profile update failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change authenticated user's password")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Principal principal) {
        try {
            String username = principal.getName();
            UserResponse user = userManagementService.getUserByUsername(username);
            userManagementService.changePassword(user.getId(), request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            logger.error("Failed to change password for user: {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Password change failed", "message", e.getMessage()));
        }
    }

    // ========================= ADMIN USER MANAGEMENT =========================

    @PostMapping("/admin/users")
    @Operation(summary = "Create user (Admin)", description = "Create a new user with specified roles (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            logger.info("Admin creating user: {}", request.getUsername());
            UserResponse response = keycloakService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Admin failed to create user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User creation failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/users")
    @Operation(summary = "Get all users (Admin)", description = "Get paginated list of all users (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Search term") @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> users = userManagementService.getAllUsers(pageable, search);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to get all users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve users", "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/users/{userId}")
    @Operation(summary = "Get user by ID (Admin)", description = "Get specific user details by ID (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        try {
            logger.info("Admin getting user by ID: {}", userId);
            UserResponse user = keycloakService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Failed to get user by ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found", "message", e.getMessage()));
        }
    }

    @PutMapping("/admin/users/{userId}")
    @Operation(summary = "Update user (Admin)", description = "Update user information (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            logger.info("Admin updating user: {}", userId);
            UserResponse updatedUser = userManagementService.updateUser(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Failed to update user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User update failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/users/{userId}/roles")
    @Operation(summary = "Assign roles to user (Admin)", description = "Assign or update user roles (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRoles(
            @PathVariable String userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        try {
            logger.info("Admin assigning roles {} to user: {}", request.getRoles(), userId);
            userManagementService.updateUserRoles(userId, request.getRoles());
            UserResponse updatedUser = keycloakService.getUserById(userId);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Failed to assign roles to user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Role assignment failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/users/{userId}/enable")
    @Operation(summary = "Enable user (Admin)", description = "Enable a disabled user account (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enableUser(@PathVariable String userId) {
        try {
            logger.info("Admin enabling user: {}", userId);
            userManagementService.enableUser(userId);
            return ResponseEntity.ok(Map.of("message", "User enabled successfully"));
        } catch (Exception e) {
            logger.error("Failed to enable user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to enable user", "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/users/{userId}/disable")
    @Operation(summary = "Disable user (Admin)", description = "Disable a user account (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disableUser(@PathVariable String userId) {
        try {
            logger.info("Admin disabling user: {}", userId);
            userManagementService.disableUser(userId);
            return ResponseEntity.ok(Map.of("message", "User disabled successfully"));
        } catch (Exception e) {
            logger.error("Failed to disable user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to disable user", "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/users/{userId}/reset-password")
    @Operation(summary = "Reset user password (Admin)", description = "Reset user password (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetUserPassword(
            @PathVariable String userId,
            @RequestParam String newPassword,
            @RequestParam(defaultValue = "false") boolean temporary) {
        try {
            logger.info("Admin resetting password for user: {}", userId);
            userManagementService.resetPassword(userId, newPassword, temporary);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            logger.error("Failed to reset password for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Password reset failed", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Delete user (Admin)", description = "Permanently delete a user account (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        try {
            logger.info("Admin deleting user: {}", userId);
            userManagementService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User deletion failed", "message", e.getMessage()));
        }
    }

    // ========================= ROLE-BASED USER QUERIES =========================

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Get users by role", description = "Get users with specific role (Admin/Staff)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            logger.info("Getting users by role: {}", role);
            List<UserResponse> users = keycloakService.getUsersByRole(role.toUpperCase());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to get users by role: {}", role, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to get users", "message", e.getMessage()));
        }
    }

    @GetMapping("/admin/users/statistics")
    @Operation(summary = "Get user statistics (Admin)", description = "Get user count statistics by role (Admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStatistics() {
        try {
            Map<String, Object> statistics = userManagementService.getUserStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Failed to get user statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get statistics", "message", e.getMessage()));
        }
    }

    // ========================= STAFF MANAGEMENT ENDPOINTS =========================

    @GetMapping("/staff/customers")
    @Operation(summary = "Get customers (Staff)", description = "Get list of customers (Staff/Admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> customers = userManagementService.getUsersByRole("CUSTOMER", pageable);
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            logger.error("Failed to get customers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get customers", "message", e.getMessage()));
        }
    }

    @GetMapping("/staff/drivers")
    @Operation(summary = "Get drivers (Staff)", description = "Get list of drivers (Staff/Admin)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> getDrivers() {
        try {
            List<UserResponse> drivers = keycloakService.getUsersByRole("DRIVER");
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            logger.error("Failed to get drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get drivers", "message", e.getMessage()));
        }
    }
}