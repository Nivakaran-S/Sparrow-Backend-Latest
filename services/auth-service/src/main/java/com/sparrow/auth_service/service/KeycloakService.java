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
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();

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

        // Create user
        var response = usersResource.create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user: " + response.getStatusInfo().getReasonPhrase());
        }

        String userId = CreatedResponseUtil.getCreatedId(response);

        // Assign roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            assignRolesToUser(userId, request.getRoles());
        }

        return getUserById(userId);
    }

    public UserResponse getUserById(String userId) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);
        UserRepresentation userRep = userResource.toRepresentation();

        return mapToUserResponse(userRep);
    }

    public List<UserResponse> getUsersByRole(String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        List<UserRepresentation> users = realmResource.users().list();

        return users.stream()
                .filter(user -> user.getRealmRoles() != null && user.getRealmRoles().contains(roleName))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public void assignRolesToUser(String userId, List<String> roleNames) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        List<RoleRepresentation> rolesToAdd = roleNames.stream()
                .map(roleName -> realmResource.roles().get(roleName).toRepresentation())
                .collect(Collectors.toList());

        userResource.roles().realmLevel().add(rolesToAdd);
    }

    public void disableUser(String userId) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(false);
        userResource.update(user);
    }

    private UserResponse mapToUserResponse(UserRepresentation userRep) {
        UserResponse response = new UserResponse();
        response.setId(userRep.getId());
        response.setUsername(userRep.getUsername());
        response.setEmail(userRep.getEmail());
        response.setFirstName(userRep.getFirstName());
        response.setLastName(userRep.getLastName());
        response.setEnabled(userRep.isEnabled());
        response.setCreatedTimestamp(new Date(userRep.getCreatedTimestamp())
                .toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());

        if (userRep.getRealmRoles() != null) {
            response.setRoles(new ArrayList<>(userRep.getRealmRoles()));
        }

        return response;
    }
}
