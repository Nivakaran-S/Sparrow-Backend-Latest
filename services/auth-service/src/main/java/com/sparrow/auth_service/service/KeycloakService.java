package com.sparrow.auth_service.service;

import com.sparrow.auth_service.dto.UserRegistrationRequest;
import com.sparrow.auth_service.dto.UserResponse;
import com.sparrow.auth_service.exception.KeycloakException;
import com.sparrow.auth_service.exception.UserAlreadyExistsException;
import com.sparrow.auth_service.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Use Jakarta EE instead of javax
import jakarta.servlet.http.HttpServletRequest;
// Use Jakarta WS-RS instead of javax
import jakarta.ws.rs.core.Response;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;
    private final AuditService auditService;

    @Value("${keycloak.realm}")
    private String realm;

    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "CUSTOMER", "STAFF", "DRIVER");

    public UserResponse createUser(UserRegistrationRequest request) {
        String performedBy = getCurrentUsername();
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();

        try {
            RealmResource realmResource = getRealmResource();
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(request.getUsername(), true);
            if (!existingUsers.isEmpty()) {
                auditService.logUserRegistration(null, request.getUsername(), ipAddress, userAgent, false);
                throw new UserAlreadyExistsException("User with username " + request.getUsername() + " already exists");
            }

            // Check email uniqueness
            List<UserRepresentation> existingByEmail = usersResource.search(null, request.getEmail(), null, null, 0, 1);
            if (!existingByEmail.isEmpty()) {
                auditService.logUserRegistration(null, request.getUsername(), ipAddress, userAgent, false);
                throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
            }

            UserRepresentation user = buildUserRepresentation(request);

            // Create user
            String userId;
            try (Response response = usersResource.create(user)) {
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    String errorMessage = extractErrorMessage(response);
                    log.error("Failed to create user {}: {}", request.getUsername(), errorMessage);
                    auditService.logUserRegistration(null, request.getUsername(), ipAddress, userAgent, false);
                    throw new KeycloakException("Failed to create user: " + errorMessage);
                }

                userId = CreatedResponseUtil.getCreatedId(response);
                log.info("User created with ID: {}", userId);
            }

            // Assign roles if specified
            List<String> rolesToAssign = request.getRoles() != null && !request.getRoles().isEmpty()
                    ? request.getRoles()
                    : Collections.singletonList("CUSTOMER");

            try {
                assignRolesToUser(userId, rolesToAssign);
                log.info("Successfully assigned roles {} to user {}", rolesToAssign, userId);
            } catch (Exception e) {
                log.error("Failed to assign roles to user {}: {}", userId, e.getMessage(), e);
                // Don't throw exception here - user was created successfully
            }

            UserResponse userResponse = getUserById(userId);

            // Log successful registration
            auditService.logUserRegistration(userId, request.getUsername(), ipAddress, userAgent, true);

            log.info("User {} created successfully with ID: {}", request.getUsername(), userId);
            return userResponse;

        } catch (UserAlreadyExistsException e) {
            throw e; // Re-throw as is
        } catch (Exception e) {
            log.error("Error creating user: {}", request.getUsername(), e);
            auditService.logUserRegistration(null, request.getUsername(), ipAddress, userAgent, false);
            throw new KeycloakException("Failed to create user: " + e.getMessage(), e);
        }
    }


    public UserResponse getUserById(String userId) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation userRep = userResource.toRepresentation();

            if (userRep == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            // Get user roles - only effective realm roles, not default ones
            List<RoleRepresentation> effectiveRealmRoles = userResource.roles().realmLevel().listEffective();
            List<String> roleNames = effectiveRealmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(roleName -> VALID_ROLES.contains(roleName))
                    .collect(Collectors.toList());

            log.debug("User {} has effective roles: {}", userRep.getUsername(), roleNames);
            return mapToUserResponse(userRep, roleNames);

        } catch (UserNotFoundException e) {
            throw e; // Re-throw as is
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", userId, e);
            throw new KeycloakException("Failed to get user: " + e.getMessage(), e);
        }
    }

    public List<UserResponse> getUsersByRole(String roleName) {
        try {
            if (!VALID_ROLES.contains(roleName.toUpperCase())) {
                throw new IllegalArgumentException("Invalid role: " + roleName);
            }

            RealmResource realmResource = getRealmResource();

            // Get all users with the specified role
            List<UserRepresentation> users = realmResource.roles().get(roleName.toUpperCase()).getUserMembers();

            return users.stream()
                    .map(userRep -> {
                        try {
                            UserResource userResource = realmResource.users().get(userRep.getId());
                            List<RoleRepresentation> effectiveRealmRoles = userResource.roles().realmLevel().listEffective();
                            List<String> roleNames = effectiveRealmRoles.stream()
                                    .map(RoleRepresentation::getName)
                                    .filter(rName -> VALID_ROLES.contains(rName))
                                    .collect(Collectors.toList());
                            return mapToUserResponse(userRep, roleNames);
                        } catch (Exception e) {
                            log.warn("Error getting roles for user: {}", userRep.getUsername(), e);
                            return mapToUserResponse(userRep, Collections.emptyList());
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting users by role: {}", roleName, e);
            throw new KeycloakException("Failed to get users by role: " + e.getMessage(), e);
        }
    }

    public void assignRolesToUser(String userId, List<String> roleNames) {
        String performedBy = getCurrentUsername();

        try {
            // Validate roles
            List<String> validRoles = roleNames.stream()
                    .filter(role -> VALID_ROLES.contains(role.toUpperCase()))
                    .map(String::toUpperCase)
                    .distinct()
                    .collect(Collectors.toList());

            if (validRoles.isEmpty()) {
                throw new IllegalArgumentException("No valid roles provided");
            }

            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            List<RoleRepresentation> rolesToAdd = new ArrayList<>();

            for (String roleName : validRoles) {
                try {
                    RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                    rolesToAdd.add(role);
                    log.debug("Found role {} for assignment", roleName);
                } catch (Exception e) {
                    log.warn("Role {} not found in realm, skipping. Error: {}", roleName, e.getMessage());
                }
            }

            if (!rolesToAdd.isEmpty()) {
                userResource.roles().realmLevel().add(rolesToAdd);

                String rolesString = String.join(", ", validRoles);
                auditService.logRoleChange(userId, user.getUsername(), rolesString, performedBy);

                log.info("Successfully assigned roles {} to user {} by {}",
                        validRoles, userId, performedBy);
            } else {
                log.warn("No valid roles found to assign to user {}", userId);
            }

        } catch (UserNotFoundException e) {
            throw e; // Re-throw as is
        } catch (Exception e) {
            log.error("Error assigning roles {} to user {}: {}", roleNames, userId, e.getMessage(), e);
            throw new KeycloakException("Failed to assign roles: " + e.getMessage(), e);
        }
    }

    public void disableUser(String userId) {
        String performedBy = getCurrentUsername();

        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            user.setEnabled(false);
            userResource.update(user);

            auditService.logUserStatusChange(userId, user.getUsername(), false, performedBy);
            log.info("User {} disabled by {}", userId, performedBy);

        } catch (UserNotFoundException e) {
            throw e; // Re-throw as is
        } catch (Exception e) {
            log.error("Error disabling user: {}", userId, e);
            throw new KeycloakException("Failed to disable user: " + e.getMessage(), e);
        }
    }

    public List<String> getUserRoles(String userId) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);

            List<RoleRepresentation> effectiveRealmRoles = userResource.roles().realmLevel().listEffective();
            List<String> roleNames = effectiveRealmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(roleName -> VALID_ROLES.contains(roleName))
                    .collect(Collectors.toList());

            log.debug("User {} effective roles: {}", userId, roleNames);
            return roleNames;

        } catch (Exception e) {
            log.error("Error getting user roles for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    public void deleteUser(String userId) {
        String performedBy = getCurrentUsername();

        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            if (user == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            String username = user.getUsername();
            userResource.remove();

            auditService.logUserDeletion(userId, username, performedBy);
            log.info("User {} ({}) deleted by {}", username, userId, performedBy);

        } catch (UserNotFoundException e) {
            throw e; // Re-throw as is
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new KeycloakException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    private RealmResource getRealmResource() {
        try {
            return keycloak.realm(realm);
        } catch (Exception e) {
            log.error("Failed to get realm resource for realm: {}", realm, e);
            throw new KeycloakException("Failed to access Keycloak realm: " + realm, e);
        }
    }

    private UserRepresentation buildUserRepresentation(UserRegistrationRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        // Set credentials
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        // Set attributes if available
        if (request.getPhoneNumber() != null || request.getAddress() != null) {
            Map<String, List<String>> attributes = new HashMap<>();
            if (request.getPhoneNumber() != null) {
                attributes.put("phoneNumber", Collections.singletonList(request.getPhoneNumber()));
            }
            if (request.getAddress() != null) {
                attributes.put("address", Collections.singletonList(request.getAddress()));
            }
            user.setAttributes(attributes);
        }