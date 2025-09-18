package com.sparrow.parcel_service.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Parcel Service API")
                        .description("API for parcel management and tracking in consolidation platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Parcel Platform Team")
                                .email("support@parcelplatform.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://parcel-service:8080")
                                .description("Docker Production Server")
                ));
    }
}