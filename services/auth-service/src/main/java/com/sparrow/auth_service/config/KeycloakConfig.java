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

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Bean
    @Lazy
    public Keycloak keycloakAdmin() {
        int maxRetries = 10;
        int baseDelay = 5000; // 5 seconds base delay
        int maxDelay = 30000; // 30 seconds max delay

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Keycloak connection attempt {}/{} to {}", attempt, maxRetries, serverUrl);

                // Use master realm for admin operations
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)
                        .realm("master")  // Connect to master realm for admin operations
                        .grantType(OAuth2Constants.PASSWORD)
                        .clientId("admin-cli")
                        .username(adminUsername)
                        .password(adminPassword)
                        .build();

                // Test the connection by getting master realm info
                keycloak.realm("master").toRepresentation();

                // Also test if we can access the target realm
                try {
                    keycloak.realm("parcel-realm").toRepresentation();
                    logger.info("✅ Successfully connected to Keycloak admin client and verified parcel-realm access");
                } catch (Exception e) {
                    logger.warn("Connected to Keycloak but parcel-realm may not be ready yet: {}", e.getMessage());
                    // Continue anyway - the realm might be imported later
                }

                return keycloak;

            } catch (Exception e) {
                logger.warn("Keycloak connection attempt {} failed: {}", attempt, e.getMessage());

                if (attempt == maxRetries) {
                    logger.error("❌ Failed to connect to Keycloak after {} attempts. Last error: {}", maxRetries, e.getMessage());
                    throw new RuntimeException("Keycloak initialization failed after " + maxRetries + " attempts: " + e.getMessage(), e);
                }

                // Exponential backoff with jitter
                int delay = Math.min(baseDelay * attempt, maxDelay);
                logger.info("Retrying Keycloak connection in {} ms...", delay);

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