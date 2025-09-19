package com.sparrow.auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8083}")
    private String serverPort;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(getApiInfo())
                .servers(getServers())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token obtained from login endpoint")
                        )
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 flow using Keycloak")
                                .flows(new io.swagger.v3.oas.models.security.OAuthFlows()
                                        .authorizationCode(new io.swagger.v3.oas.models.security.OAuthFlow()
                                                .authorizationUrl(keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/auth")
                                                .tokenUrl(keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                                                .scopes(new io.swagger.v3.oas.models.security.Scopes()
                                                        .addString("openid", "OpenID Connect scope")
                                                        .addString("profile", "Profile information")
                                                        .addString("email", "Email address")
                                                )
                                        )
                                )
                        )
                );
    }

    private Info getApiInfo() {
        return new Info()
                .title("Sparrow Auth Service API")
                .description("""
                    # Authentication & User Management Service
                    
                    This service provides comprehensive authentication and user management capabilities for the Sparrow Parcel Consolidation Platform.
                    
                    ## Features
                    - **Authentication**: Login, logout, token refresh
                    - **User Registration**: Public customer registration  
                    - **Profile Management**: Update profile, change password
                    - **Admin User Management**: Create, update, delete users
                    - **Role Management**: Assign and manage user roles
                    - **Audit Logging**: Track user activities and system events
                    
                    ## User Roles
                    - **ADMIN**: Full system administration privileges
                    - **STAFF**: Staff operations and customer management
                    - **DRIVER**: Driver-specific operations
                    - **CUSTOMER**: End-user customer operations
                    
                    ## Authentication
                    Most endpoints require JWT Bearer authentication. Obtain tokens via the `/api/auth/login` endpoint.
                    
                    ## Rate Limiting
                    API calls are rate-limited to prevent abuse. Check response headers for current limits.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Sparrow Development Team")
                        .email("dev@sparrow.com")
                        .url("https://sparrow.com")
                )
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT")
                );
    }

    private List<Server> getServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Development Server"),
                new Server()
                        .url("http://localhost:8080/auth-service")
                        .description("Local API Gateway"),
                new Server()
                        .url("https://api.sparrow.com/auth-service")
                        .description("Production Server")
        );
    }
}

// Additional configuration class for security headers and CORS
