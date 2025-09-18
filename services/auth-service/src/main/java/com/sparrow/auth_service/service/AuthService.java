package com.sparrow.auth_service.service;


import com.sparrow.auth_service.dto.AuthResponse;
import com.sparrow.auth_service.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public AuthResponse login(LoginRequest request) {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();

            AuthResponse response = new AuthResponse();
            response.setAccessToken(tokenResponse.getToken());
            response.setRefreshToken(tokenResponse.getRefreshToken());
            response.setTokenType("Bearer");
            response.setExpiresIn(tokenResponse.getExpiresIn());
            response.setUsername(request.getUsername());

            return response;

        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getUsername(), e);
            throw new RuntimeException("Invalid credentials");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .grantType(OAuth2Constants.REFRESH_TOKEN) // correct grant type
                    .build();

            // This is how you refresh using the existing refresh token
            AccessTokenResponse tokenResponse = keycloak.tokenManager().refreshToken();

            AuthResponse response = new AuthResponse();
            response.setAccessToken(tokenResponse.getToken());
            response.setRefreshToken(tokenResponse.getRefreshToken());
            response.setTokenType("Bearer");
            response.setExpiresIn(tokenResponse.getExpiresIn());

            return response;

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RuntimeException("Invalid refresh token");
        }
    }

}