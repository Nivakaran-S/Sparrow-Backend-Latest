package com.sparrow.auth_service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class JwtUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractUsername(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // Split the token
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid JWT token structure");
            }

            // Decode the payload
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decodedPayload);

            // Parse JSON and extract preferred_username
            JsonNode jsonNode = objectMapper.readTree(payload);

            // Try preferred_username first (Keycloak standard)
            if (jsonNode.has("preferred_username")) {
                return jsonNode.get("preferred_username").asText();
            }

            // Fallback to sub claim
            if (jsonNode.has("sub")) {
                return jsonNode.get("sub").asText();
            }

            throw new RuntimeException("Username not found in token");

        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            throw new RuntimeException("Failed to extract username from token: " + e.getMessage());
        }
    }

    public JsonNode parseTokenPayload(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // Split the token
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid JWT token structure");
            }

            // Decode the payload
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decodedPayload);

            return objectMapper.readTree(payload);

        } catch (Exception e) {
            log.error("Error parsing token payload", e);
            throw new RuntimeException("Failed to parse token: " + e.getMessage());
        }
    }
}