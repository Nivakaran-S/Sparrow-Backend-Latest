package com.sparrow.auth_service.service;

import com.sparrow.auth_service.dto.PasswordChangeRequest;
import com.sparrow.auth_service.dto.UserResponse;
import com.sparrow.auth_service.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final Keycloak keycloak;
    private final KeycloakService keycloakService;

    @Value("${keycloak.realm}")
    private String realm;

    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "CUSTOMER", "STAFF", "DRIVER");

    public Page<UserResponse> getAllUsers(Pageable pageable, String search) {
        try {
            RealmResource realmResource = getRealmResource();
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> allUsers;

            if (search != null && !search.trim().isEmpty()) {
                // Search by username, email, first name, or last name
                allUsers = usersResource.search(search.trim(), null, null, null, null, null);
            } else {
                // Get all users with pagination
                int offset = (int) pageable.getOffset();
                int limit = pageable.getPageSize();
                allUsers = usersResource.list(offset, limit);
            }

            List<UserResponse> userResponses = allUsers.stream()
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());

            // Manual pagination for search results
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), userResponses.size());

            List<UserResponse> pageContent = start < userResponses.size()
                    ? userResponses.subList(start, end)
                    : new ArrayList<>();

            return new PageImpl<>(pageContent, pageable, userResponses.size());

        } catch (Exception e) {
            log.error("Error getting all users", e);
            throw new RuntimeException("Failed to retrieve users: " + e.getMessage());
        }
    }

    public Page<UserResponse> getUsersByRole(String role, Pageable pageable) {
        try {
            List<UserResponse> users = keycloakService.getUsersByRole(role);

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), users.size());

            List<UserResponse> pageContent = start < users.size()
                    ? users.subList(start, end)
                    : new ArrayList<>();

            return new PageImpl<>(pageContent, pageable, users.size());

        } catch (Exception e) {
            log.error("Error getting users by role: {}", role, e);
            throw new RuntimeException("Failed to retrieve users by role: " + e.getMessage());
        }
    }

    public UserResponse getUserByUsername(String username) {
        try {
            RealmResource realmResource = getRealmResource();
            List<UserRepresentation> users = realmResource.users()
                    .search(username, null, null, null, 0, 1);

            UserRepresentation user = users.stream()
                    .filter(u -> username.equals(u.getUsername()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            return convertToUserResponse(user);

        } catch (Exception e) {
            log.error("Error getting user by username: {}", username, e);
            throw new RuntimeException("Failed to get user: " + e.getMessage());
        }
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Update basic fields
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }

            // Update attributes
            Map<String, List<String>> attributes = user.getAttributes() != null
                    ? user.getAttributes()
                    : new HashMap<>();

            if (request.getPhoneNumber() != null) {
                attributes.put("phoneNumber", List.of(request.getPhoneNumber()));
            }
            if (request.getAddress() != null) {
                attributes.put("address", List.of(request.getAddress()));
            }

            // Add additional attributes if provided
            if (request.getAdditionalAttributes() != null) {
                request.getAdditionalAttributes().forEach((key, value) ->
                        attributes.put(key, List.of(value)));
            }

            user.setAttributes(attributes);

            userResource.update(user);

            log.info("User {} updated successfully", userId);
            return keycloakService.getUserById(userId);

        } catch (Exception e) {
            log.error("Error updating user: {}", userId, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage());
        }
    }

    public void updateUserRoles(String userId, List<String> newRoles) {
        try {
            // Validate roles
            List<String> validRoles = newRoles.stream()
                    .filter(role -> VALID_ROLES.contains(role.toUpperCase()))
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

            if (validRoles.isEmpty()) {
                throw new RuntimeException("No valid roles provided");
            }

            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);

            // Remove all current realm roles (except default ones)
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
            List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                    .filter(role -> VALID_ROLES.contains(role.getName()))
                    .collect(Collectors.toList());

            if (!rolesToRemove.isEmpty()) {
                userResource.roles().realmLevel().remove(rolesToRemove);
            }

            // Add new roles
            List<RoleRepresentation> rolesToAdd = new ArrayList<>();
            for (String roleName : validRoles) {
                try {
                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    rolesToAdd.add(role);
                } catch (Exception e) {
                    log.warn("Role {} not found, skipping", roleName);
                }
            }

            if (!rolesToAdd.isEmpty()) {
                userResource.roles().realmLevel().add(rolesToAdd);
                log.info("Updated roles for user {}: {}", userId, validRoles);
            }

        } catch (Exception e) {
            log.error("Error updating user roles for user: {}", userId, e);
            throw new RuntimeException("Failed to update user roles: " + e.getMessage());
        }
    }

    public void changePassword(String userId, PasswordChangeRequest request) {
        try {
            if (!request.isPasswordMatching()) {
                throw new RuntimeException("Password and confirmation do not match");
            }

            // In a real implementation, you might want to verify the current password
            // For now, we'll just update the password directly
            resetPassword(userId, request.getNewPassword(), false);

            log.info("Password changed successfully for user: {}", userId);

        } catch (Exception e) {
            log.error("Error changing password for user: {}", userId, e);
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }

    public void resetPassword(String userId, String newPassword, boolean temporary) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(temporary);

            userResource.resetPassword(credential);

            log.info("Password reset successfully for user: {} (temporary: {})", userId, temporary);

        } catch (Exception e) {
            log.error("Error resetting password for user: {}", userId, e);
            throw new RuntimeException("Failed to reset password: " + e.getMessage());
        }
    }

    public void enableUser(String userId) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            user.setEnabled(true);
            userResource.update(user);

            log.info("User {} enabled successfully", userId);

        } catch (Exception e) {
            log.error("Error enabling user: {}", userId, e);
            throw new RuntimeException("Failed to enable user: " + e.getMessage());
        }
    }

    public void disableUser(String userId) {
        try {
            keycloakService.disableUser(userId);
            log.info("User {} disabled successfully", userId);

        } catch (Exception e) {
            log.error("Error disabling user: {}", userId, e);
            throw new RuntimeException("Failed to disable user: " + e.getMessage());
        }
    }

    public void deleteUser(String userId) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);

            // Get user info for logging before deletion
            UserRepresentation user = userResource.toRepresentation();
            String username = user.getUsername();

            userResource.remove();

            log.info("User {} ({}) deleted successfully", username, userId);

        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }

    public Map<String, Object> getUserStatistics() {
        try {
            RealmResource realmResource = getRealmResource();
            Map<String, Object> statistics = new HashMap<>();

            // Get total user count
            int totalUsers = realmResource.users().count();
            statistics.put("totalUsers", totalUsers);

            // Get count by roles
            Map<String, Integer> roleStats = new HashMap<>();
            for (String role : VALID_ROLES) {
                try {
                    List<UserRepresentation> usersWithRole = realmResource.roles().get(role).getUserMembers();
                    roleStats.put(role.toLowerCase(), usersWithRole.size());
                } catch (Exception e) {
                    log.warn("Error getting count for role: {}", role, e);
                    roleStats.put(role.toLowerCase(), 0);
                }
            }
            statistics.put("usersByRole", roleStats);

            // Get enabled/disabled count
            List<UserRepresentation> allUsers = realmResource.users().list();
            long enabledCount = allUsers.stream().mapToLong(user -> user.isEnabled() ? 1 : 0).sum();
            long disabledCount = totalUsers - enabledCount;

            statistics.put("enabledUsers", enabledCount);
            statistics.put("disabledUsers", disabledCount);

            // Get recent registrations (last 30 days)
            long thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 3600).toEpochMilli();
            long recentRegistrations = allUsers.stream()
                    .filter(user -> user.getCreatedTimestamp() != null && user.getCreatedTimestamp() > thirtyDaysAgo)
                    .count();
            statistics.put("recentRegistrations", recentRegistrations);

            return statistics;

        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            throw new RuntimeException("Failed to get user statistics: " + e.getMessage());
        }
    }

    private RealmResource getRealmResource() {
        try {
            return keycloak.realm(realm);
        } catch (Exception e) {
            log.error("Failed to get realm resource for realm: {}", realm, e);
            throw new RuntimeException("Failed to access Keycloak realm: " + realm + ". Error: " + e.getMessage());
        }
    }

    private UserResponse convertToUserResponse(UserRepresentation userRep) {
        try {
            // Get user roles
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userRep.getId());
            List<RoleRepresentation> effectiveRealmRoles = userResource.roles().realmLevel().listEffective();

            List<String> roleNames = effectiveRealmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(roleName -> VALID_ROLES.contains(roleName))
                    .collect(Collectors.toList());

            UserResponse response = new UserResponse();
            response.setId(userRep.getId());
            response.setUsername(userRep.getUsername());
            response.setEmail(userRep.getEmail());
            response.setFirstName(userRep.getFirstName());
            response.setLastName(userRep.getLastName());
            response.setEnabled(userRep.isEnabled());
            response.setRoles(roleNames);

            // Convert timestamp
            if (userRep.getCreatedTimestamp() != null) {
                response.setCreatedTimestamp(
                        Instant.ofEpochMilli(userRep.getCreatedTimestamp())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                );
            }

            // Set attributes
            if (userRep.getAttributes() != null) {
                Map<String, List<String>> attributes = userRep.getAttributes();
                if (attributes.containsKey("phoneNumber") && !attributes.get("phoneNumber").isEmpty()) {
                    response.setPhoneNumber(attributes.get("phoneNumber").get(0));
                }
                if (attributes.containsKey("address") && !attributes.get("address").isEmpty()) {
                    response.setAddress(attributes.get("address").get(0));
                }
            }

            return response;

        } catch (Exception e) {
            log.warn("Error converting user representation for user: {}", userRep.getUsername(), e);
            // Return basic user info if role lookup fails
            UserResponse response = new UserResponse();
            response.setId(userRep.getId());
            response.setUsername(userRep.getUsername());
            response.setEmail(userRep.getEmail());
            response.setFirstName(userRep.getFirstName());
            response.setLastName(userRep.getLastName());
            response.setEnabled(userRep.isEnabled());
            response.setRoles(Collections.emptyList());
            return response;
        }
    }
}