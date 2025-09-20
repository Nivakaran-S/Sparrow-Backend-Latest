package com.sparrow.auth_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize annotations
public class SecurityConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Remove CORS configuration - let API Gateway handle it
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers(
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/webjars/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh"
                        ).permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")

                        // Staff and Admin endpoints
                        .requestMatchers("/api/auth/staff/**").hasAnyRole("STAFF", "ADMIN")

                        // Profile management - authenticated users only
                        .requestMatchers(
                                "/api/auth/profile",
                                "/api/auth/change-password",
                                "/api/auth/logout"
                        ).authenticated()

                        // Role-based user queries
                        .requestMatchers("/api/auth/users/role/**").hasAnyRole("ADMIN", "STAFF")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwkSetUri(keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/certs")
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract roles from the JWT token
            Collection<String> roles = extractRoles(jwt);

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        // Set the principal name claim (username)
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractRoles(org.springframework.security.oauth2.jwt.Jwt jwt) {
        try {
            // Try to extract from realm_access.roles first (standard Keycloak format)
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                return (Collection<String>) realmAccess.get("roles");
            }

            // Try to extract from roles claim (custom format)
            Collection<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null && !roles.isEmpty()) {
                return roles;
            }

            // If no roles found, return empty collection
            return Collections.emptyList();

        } catch (Exception e) {
            // Log the error and return empty collection
            System.err.println("Error extracting roles from JWT: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}