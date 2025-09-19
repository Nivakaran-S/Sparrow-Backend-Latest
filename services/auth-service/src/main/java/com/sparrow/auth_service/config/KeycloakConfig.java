package com.sparrow.auth_service.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class KeycloakConfig {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakConfig.class);

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.client-secret:}")
    private String adminClientSecret;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Bean
    @Lazy
    public Keycloak keycloakAdmin() {
        int maxRetries = 5;
        int baseDelay = 2000; // 2 seconds base delay
        int maxDelay = 10000; // 10 seconds max delay

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Keycloak connection attempt {}/{} to {}", attempt, maxRetries, serverUrl);

                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)
                        .realm(realm)
                        .grantType(OAuth2Constants.PASSWORD)
                        .clientId(adminClientId)
                        .username(adminUsername)
                        .password(adminPassword)
                        .build();

                // Test the connection by getting realm info
                keycloak.realm(realm).toRepresentation();

                logger.info("✅ Successfully connected to Keycloak admin client");
                return keycloak;

            } catch (Exception e) {
                if (attempt == maxRetries) {
                    logger.error("❌ Failed to connect to Keycloak after {} attempts: {}", maxRetries, e.getMessage());
                    throw new RuntimeException("Keycloak initialization failed: " + e.getMessage(), e);
                }

                // Exponential backoff with jitter
                int delay = Math.min(baseDelay * (int) Math.pow(2, attempt - 1), maxDelay);
                logger.warn("Keycloak connection failed, retrying in {} ms: {}", delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during Keycloak connection retry", ie);
                }
            }
        }

        throw new RuntimeException("Failed to connect to Keycloak after " + maxRetries + " attempts");
    }
}