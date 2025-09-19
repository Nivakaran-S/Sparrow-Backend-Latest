package com.sparrow.auth_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class JwtUtils {

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
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // Parse the JSON payload and extract preferred_username
            // This is a simplified approach - in production, use a proper JWT library
            if (payload.contains("\"preferred_username\":")) {
                int start = payload.indexOf("\"preferred_username\":") + "\"preferred_username\":".length() + 1;
                int end = payload.indexOf("\"", start);
                return payload.substring(start, end);
            }

            throw new RuntimeException("Username not found in token");
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            throw new RuntimeException("Failed to extract username from token: " + e.getMessage());
        }
    }

    public Claims parseToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // Parse without signature verification (for basic info extraction)
            return Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(token) // Use parseClaimsJwt for unsigned part
                    .getBody();
        } catch (Exception e) {
            log.error("Error parsing token", e);
            throw new RuntimeException("Failed to parse token: " + e.getMessage());
        }
    }
}