package com.sparrow.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Spring Boot Services (using Eureka service discovery)
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://auth-service"))

                .route("pricing-service", r -> r.path("/api/pricing/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://pricing-service"))

                .route("payment-service", r -> r.path("/api/payments/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://payment-service"))

                .route("consolidation-service", r -> r.path("/api/consolidation/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://consolidation-service"))

                .route("warehouse-service", r -> r.path("/api/warehouses/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://warehouse-service"))

                // Java Parcel Service (runs on port 8080 internally)
                .route("parcel-service", r -> r.path("/api/parcels/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://parcel-service:8080"))

                // Python Services (both run on port 8080 internally)
                .route("chatbot-service", r -> r.path("/api/chatbot/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://sparrow-agent:8080"))  // Note: container name is sparrow-agent

                .route("eta-service", r -> r.path("/api/eta/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://eta-service:8080"))

                // External Services
                .route("kafka-ui", r -> r.path("/kafka-ui/**")
                        .filters(f -> f.rewritePath("/kafka-ui/(?<segment>.*)", "/${segment}"))
                        .uri("http://kafka-ui:8080"))

                .route("keycloak-admin", r -> r.path("/auth/admin/**")
                        .filters(f -> f.rewritePath("/auth/admin/(?<segment>.*)", "/admin/${segment}"))
                        .uri("http://keycloak:8080"))

                .build();
    }


}