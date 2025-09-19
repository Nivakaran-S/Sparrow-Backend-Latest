package com.sparrow.auth_service.service;

import com.sparrow.auth_service.dto.AuthResponse;
import com.sparrow.auth_service.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.NotAuthorizedException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final Keycloak adminKeycloak;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthResponse login(LoginRequest request) {
        try {
            // Create a user-specific Keycloak instance for login
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            // Get access token
            AccessTokenResponse tokenResponse = userKeycloak.tokenManager().getAccessToken();

            // Get user details from admin client
            UserRepresentation user = findUserByUsername(request.getUsername());

            if (user == null) {
                throw new RuntimeException("User not found after successful login");
            }

            // Build response
            AuthResponse response = new AuthResponse();
            response.setAccessToken(tokenResponse.getToken());
            response.setRefreshToken(tokenResponse.getRefreshToken());
            response.setTokenType("Bearer");
            response.setExpiresIn(tokenResponse.getExpiresIn());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setUserId(user.getId());

            if (user.getRealmRoles() != null) {
                response.setRoles(user.getRealmRoles());
            }

            log.info("User {} logged in successfully", request.getUsername());
            return response;

        } catch (NotAuthorizedException e) {
            log.warn("Invalid credentials for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(tokenUrl, request, Map.class);
            Map<String, Object> tokenResponse = responseEntity.getBody();

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new RuntimeException("Invalid refresh token");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String newRefreshToken = (String) tokenResponse.get("refresh_token");

            // Extract username from JWT
            String username = extractUsernameFromToken(accessToken);

            UserRepresentation user = findUserByUsername(username);
            if (user == null) {
                throw new RuntimeException("User not found during token refresh");
            }

            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken(accessToken);
            authResponse.setRefreshToken(newRefreshToken);
            authResponse.setTokenType("Bearer");
            authResponse.setExpiresIn(Long.valueOf((Integer) tokenResponse.get("expires_in")));
            authResponse.setUsername(user.getUsername());
            authResponse.setEmail(user.getEmail());
            authResponse.setUserId(user.getId());
            if (user.getRealmRoles() != null) {
                authResponse.setRoles(user.getRealmRoles());
            }

            log.info("Token refreshed successfully for user: {}", username);
            return authResponse;

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RuntimeException("Invalid refresh token: " + e.getMessage());
        }
    }

    private UserRepresentation findUserByUsername(String username) {
        try {
            return adminKeycloak.realm(realm).users()
                    .search(username, null, null, null, 0, 1)
                    .stream()
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error finding user by username: {}", username, e);
            return null;
        }
    }

    private String extractUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid JWT token");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            // Keycloak username claim is usually "preferred_username"
            return (String) claims.get("preferred_username");
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            throw new RuntimeException("Failed to extract username from token");
        }
    }
}
