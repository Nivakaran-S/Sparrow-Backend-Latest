package com.sparrow.api_gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Parcel Consolidation API Gateway",
                version = "1.0",
                description = "API Gateway for Parcel Consolidation and Tracking System"
        ),
        servers = {
                @Server(url = "/", description = "Gateway Server")
        }
)
@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth-service")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi pricingApi() {
        return GroupedOpenApi.builder()
                .group("pricing-service")
                .pathsToMatch("/api/pricing/**")
                .build();
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("payment-service")
                .pathsToMatch("/api/payments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi parcelApi() {
        return GroupedOpenApi.builder()
                .group("parcel-service")
                .pathsToMatch("/api/parcels/**")
                .build();
    }

    @Bean
    public GroupedOpenApi consolidationApi() {
        return GroupedOpenApi.builder()
                .group("consolidation-service")
                .pathsToMatch("/api/consolidation/**")
                .build();
    }

    @Bean
    public GroupedOpenApi warehouseApi() {
        return GroupedOpenApi.builder()
                .group("warehouse-service")
                .pathsToMatch("/api/warehouses/**")
                .build();
    }

    @Bean
    public GroupedOpenApi etaApi() {
        return GroupedOpenApi.builder()
                .group("eta-service")
                .pathsToMatch("/api/eta/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chatbotApi() {
        return GroupedOpenApi.builder()
                .group("chatbot-service")
                .pathsToMatch("/api/chatbot/**")
                .build();
    }
}