package com.sparrow.auth_service.service;

import com.sparrow.auth_service.dto.UserRegistrationRequest;
import com.sparrow.auth_service.dto.UserResponse;
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

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public UserResponse createUser(UserRegistrationRequest request) {
        try {
            RealmResource realmResource = getRealmResource();
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existingUsers = usersResource.search(request.getUsername(), true);
            if (!existingUsers.isEmpty()) {
                throw new RuntimeException("User with username " + request.getUsername() + " already exists");
            }

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

            // Create user
            try (Response response = usersResource.create(user)) {
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    String errorMessage = "Failed to create user: " + response.getStatusInfo().getReasonPhrase();
                    if (response.hasEntity()) {
                        errorMessage += " - " + response.readEntity(String.class);
                    }
                    log.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }

                String userId = CreatedResponseUtil.getCreatedId(response);

                // Assign roles if specified
                if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                    try {
                        assignRolesToUser(userId, request.getRoles());
                    } catch (Exception e) {
                        log.warn("Failed to assign roles to user {}, but user was created: {}", userId, e.getMessage());
                    }
                }

                log.info("User {} created successfully with ID: {}", request.getUsername(), userId);
                return getUserById(userId);
            }
        } catch (Exception e) {
            log.error("Error creating user: {}", request.getUsername(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    public UserResponse getUserById(String userId) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation userRep = userResource.toRepresentation();

            // Get user roles
            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listAll();
            List<String> roleNames = realmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());

            return mapToUserResponse(userRep, roleNames);
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", userId, e);
            throw new RuntimeException("User not found: " + e.getMessage());
        }
    }

    public List<UserResponse> getUsersByRole(String roleName) {
        try {
            RealmResource realmResource = getRealmResource();

            // Get all users with the specified role
            List<UserRepresentation> users = realmResource.roles().get(roleName).getUserMembers();

            return users.stream()
                    .map(userRep -> {
                        try {
                            UserResource userResource = realmResource.users().get(userRep.getId());
                            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listAll();
                            List<String> roleNames = realmRoles.stream()
                                    .map(RoleRepresentation::getName)
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
            throw new RuntimeException("Failed to get users by role: " + e.getMessage());
        }
    }

    public void assignRolesToUser(String userId, List<String> roleNames) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);

            List<RoleRepresentation> rolesToAdd = roleNames.stream()
                    .map(roleName -> {
                        try {
                            return realmResource.roles().get(roleName).toRepresentation();
                        } catch (Exception e) {
                            log.warn("Role {} not found, skipping", roleName);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!rolesToAdd.isEmpty()) {
                userResource.roles().realmLevel().add(rolesToAdd);
                log.info("Assigned roles {} to user {}", roleNames, userId);
            }
        } catch (Exception e) {
            log.error("Error assigning roles to user: {}", userId, e);
            throw new RuntimeException("Failed to assign roles: " + e.getMessage());
        }
    }

    public void disableUser(String userId) {
        try {
            RealmResource realmResource = getRealmResource();
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(false);
            userResource.update(user);
            log.info("User {} disabled", userId);
        } catch (Exception e) {
            log.error("Error disabling user: {}", userId, e);
            throw new RuntimeException("Failed to disable user: " + e.getMessage());
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

    private UserResponse mapToUserResponse(UserRepresentation userRep, List<String> roles) {
        UserResponse response = new UserResponse();
        response.setId(userRep.getId());
        response.setUsername(userRep.getUsername());
        response.setEmail(userRep.getEmail());
        response.setFirstName(userRep.getFirstName());
        response.setLastName(userRep.getLastName());
        response.setEnabled(userRep.isEnabled());

        // Convert timestamp
        if (userRep.getCreatedTimestamp() != null) {
            response.setCreatedTimestamp(
                    Instant.ofEpochMilli(userRep.getCreatedTimestamp())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
            );
        }

        // Set roles
        response.setRoles(roles);

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
    }
}