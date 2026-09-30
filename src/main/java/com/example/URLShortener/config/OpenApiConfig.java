package com.example.URLShortener.config;

import com.example.URLShortener.dto.response.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "URL Shortener API",
                version = "v1",
                description = """
                        REST API for URL Shortener platform: create/ manage short links, redirect, click analytic, user management & role
                        
                        **Authentication** send `POST /api/v1/auth/login` to get `accessToken`, click **Authorize**
                        and paste token).
                        Access token alive for 15 minutes; if the token expires, send `POST /api/v1/auth/refresh`.

                        **Error code:** every error — include `429` do rate limit — also return `ErrorResponse`
                        `{time, status, errorMessage, errorCode, path}` with `errorCode` belong to `ErrorCode`.
                        """,
                contact = @Contact(name = "Nguyen Van Phuc", url = "https://github.com/nguyenphuchntt")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local dev"),
                @Server(url = "/", description = "Same origin")
        },
        tags = {
                @Tag(name = "Auth", description = "Login, signup, refresh, logout"),
                @Tag(name = "Users", description = "User use cases"),
                @Tag(name = "Links", description = "Links management"),
                @Tag(name = "Analytics", description = "Click analytic"),
                @Tag(name = "Admin", description = "User management")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste access token received from `POST /api/v1/auth/login`"
)
public class OpenApiConfig {

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    @Bean
    public OpenApiCustomizer sharedErrorResponsesCustomizer() {
        return openApi -> {
            openApi.setComponents(openApi.getComponents() == null
                    ? new Components()
                    : openApi.getComponents());

            registerErrorSchema(openApi);
            addSharedError(openApi, "BadRequest", "Invalid data or arguments");
            addSharedError(openApi, "Unauthorized", "Not authenticated, or the token is invalid/expired");
            addSharedError(openApi, "Forbidden", "Authenticated but lacking sufficient permissions");
            addSharedError(openApi, "NotFound", "Resource not found");
            addSharedError(openApi, "Conflict", "Resource already exists");
            addSharedError(openApi, "Gone", "The short link has expired");
            addSharedError(openApi, "TooManyRequests", "Request rate limit exceeded");
            addSharedError(openApi, "ServerError", "Unexpected server-side error");

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getResponses() == null) {
                            operation.setResponses(new ApiResponses());
                        }
                        addIfAbsent(operation.getResponses(), "429", "#/components/responses/TooManyRequests");
                        addIfAbsent(operation.getResponses(), "500", "#/components/responses/ServerError");
                    }));
        };
    }

    private static void registerErrorSchema(OpenAPI openApi) {
        ModelConverters.getInstance()
                .readAll(ErrorResponse.class)
                .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
    }

    private static void addSharedError(OpenAPI openApi, String name, String description) {
        openApi.getComponents().addResponses(name, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF)))));
    }

    private static void addIfAbsent(ApiResponses responses, String code, String ref) {
        if (!responses.containsKey(code)) {
            // ApiResponses is a LinkedHashMap<String, ApiResponse>
            responses.put(code, new ApiResponse().$ref(ref));
        }
    }
}
