package com.sparrow.api_gateway.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service Routes
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service:8083"))

                // Pricing Service Routes
                .route("pricing-service", r -> r.path("/api/pricing/**")
                        .uri("lb://pricing-service:8086"))

                // Payment Service Routes
                .route("payment-service", r -> r.path("/api/payments/**")
                        .uri("lb://payment-service:8090"))

                // Parcel Service Routes
                .route("parcel-service", r -> r.path("/api/parcels/**")
                        .uri("lb://parcel-service:8084"))

                // Consolidation Service Routes
                .route("consolidation-service", r -> r.path("/api/consolidation/**")
                        .uri("lb://consolidation-service:8081"))

                // Warehouse Service Routes
                .route("warehouse-service", r -> r.path("/api/warehouses/**")
                        .uri("lb://warehouse-service:8082"))

                // ETA Service Routes
                .route("eta-service", r -> r.path("/api/eta/**")
                        .uri("lb://eta-service:8087"))

                // Chatbot Service Routes
                .route("chatbot-service", r -> r.path("/api/chatbot/**")
                        .uri("lb://chatbot-service:8088"))

                // Kafka UI
                .route("kafka-ui", r -> r.path("/kafka-ui/**")
                        .filters(f -> f.rewritePath("/kafka-ui/(?<segment>.*)", "/${segment}"))
                        .uri("http://kafka-ui:8080"))

                // Keycloak Admin Console
                .route("keycloak-admin", r -> r.path("/auth/admin/**")
                        .filters(f -> f.rewritePath("/auth/admin/(?<segment>.*)", "/admin/${segment}"))
                        .uri("http://keycloak:8080"))

                .build();
    }
}