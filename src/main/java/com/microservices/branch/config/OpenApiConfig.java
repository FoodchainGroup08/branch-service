package com.microservices.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FoodChain — Order Service API")
                        .description("Handles order placement, status updates, " +
                                "and the full order lifecycle")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("FoodchainGroup08")
                                .email("team@foodchain.com"))
                        .license(new License()
                                .name("Capstone Project 2026")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("API Gateway (use this for all requests)"),
                        new Server()
                                .url("http://localhost:8083")
                                .description("Order Service direct")
                ));
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FoodChain — Order Service API")
                        .version("v1.0.0"))

                // adds Authorize button to Swagger UI
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here. " +
                                                "Get it from POST /api/auth/login")));
    }
}