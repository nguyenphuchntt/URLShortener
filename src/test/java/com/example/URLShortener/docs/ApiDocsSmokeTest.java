package com.example.URLShortener.docs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ApiDocsSmokeTest {

    private static final Set<String> DECLARED_TAGS = Set.of("Auth", "Users", "Links", "Analytics", "Admin");

    private static final Set<String> SHARED_ERROR_RESPONSES = Set.of(
            "BadRequest", "Unauthorized", "Forbidden", "NotFound",
            "Conflict", "Gone", "TooManyRequests", "ServerError");

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/api/v1/auth/", "/r/");

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private JsonNode spec;
    private Map<String, JsonNode> operations;

    @BeforeEach
    void fetchSpec() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        spec = objectMapper.readTree(result.getResponse().getContentAsString());
        operations = new LinkedHashMap<>();
        spec.path("paths").properties().forEach(path ->
                path.getValue().properties().forEach(method ->
                        operations.put(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue())));
    }

    @Test
    @DisplayName("spec has exact 5 metadata: title, 5 tag, security scheme bearer")
    void shouldExposeMetadata() {
        assertThat(spec.path("info").path("title").asText()).isEqualTo("URL Shortener API");

        Set<String> declared = new LinkedHashSet<>();
        spec.path("tags").forEach(tag -> declared.add(tag.path("name").asText()));
        assertThat(declared).containsExactlyInAnyOrderElementsOf(DECLARED_TAGS);

        JsonNode bearer = spec.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(bearer.path("type").asText()).isEqualTo("http");
        assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearer.path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("spec fully cover 23 operation / 20 path")
    void shouldCoverEveryOperation() {
        assertThat(spec.path("paths").size()).isEqualTo(20);
        assertThat(operations).hasSize(23);
    }

    @Test
    @DisplayName("every operation has a summary and tag belong to 5 defined tags")
    void everyOperationShouldBeDocumented() {
        operations.forEach((key, operation) -> {
            assertThat(operation.path("summary").asText())
                    .as("%s missing summary", key).isNotBlank();

            List<String> tags = new ArrayList<>();
            operation.path("tags").forEach(tag -> tags.add(tag.asText()));
            assertThat(tags).as("%s missing tag", key).isNotEmpty();
            assertThat(DECLARED_TAGS).as("%s use a not declared tag", key).containsAll(tags);
        });
    }

    @Test
    @DisplayName("every operations must declare all error contract")
    void everyOperationShouldDocumentSharedErrors() throws Exception {
        operations.forEach((key, operation) -> {
            assertThat(operation.path("responses").has("429"))
                    .as("%s thiếu 429", key).isTrue();
            assertThat(operation.path("responses").has("500"))
                    .as("%s thiếu 500", key).isTrue();
        });

        Set<String> registered = new HashSet<>();
        spec.path("components").path("responses").properties()
                .forEach(entry -> registered.add(entry.getKey()));
        assertThat(registered).containsExactlyInAnyOrderElementsOf(SHARED_ERROR_RESPONSES);
    }

    @Test
    @DisplayName("operation protected require bearer, not operation public")
    void securityShouldMatchSecurityConfig() {
        operations.forEach((key, operation) -> {
            boolean isPublic = PUBLIC_PATH_PREFIXES.stream()
                    .anyMatch(prefix -> key.substring(key.indexOf(' ') + 1).startsWith(prefix));
            boolean hasBearer = operation.path("security").toString().contains("bearerAuth");

            if (isPublic) {
                assertThat(hasBearer).as("%s is public, so don't require bearer", key).isFalse();
            } else {
                assertThat(hasBearer).as("%s missing security bearerAuth", key).isTrue();
            }
        });
    }
    @Test
    @DisplayName("declared status codes match the real controller behavior")
    void shouldDocumentRealStatusCodes() {
        assertThat(operations.get("POST /api/v1/auth/register").path("responses").has("201"))
                .as("register returns 201").isTrue();
        assertThat(operations.get("POST /api/v1/urls").path("responses").has("201"))
                .as("create link returns 201").isTrue();
        assertThat(operations.get("POST /api/v1/roles").path("responses").has("201"))
                .as("create role returns 201").isTrue();

        List.of("POST /api/v1/auth/logout", "PATCH /api/v1/users/me/password",
                        "DELETE /api/v1/urls/{shortCode}", "DELETE /api/v1/admin/users/{id}")
                .forEach(key -> {
                    JsonNode responses = operations.get(key).path("responses");
                    assertThat(responses.has("204")).as("%s must have 204", key).isTrue();
                    assertThat(responses.has("200")).as("%s must not have 200", key).isFalse();
                });

        JsonNode redirect = operations.get("GET /r/{shortCode}").path("responses");
        assertThat(redirect.has("302")).as("redirect must be 302").isTrue();
        assertThat(redirect.path("302").path("headers").has("Location"))
                .as("302 must declare the Location header").isTrue();
        assertThat(redirect.has("200")).as("redirect must not have 200").isFalse();
    }

    @Test
    @DisplayName("Pageable is expanded into page/size/sort instead of a single $ref parameter")
    void pageableShouldBeExpanded() {
        List.of("GET /api/v1/urls/me", "GET /api/v1/admin/users").forEach(key -> {
            List<String> params = new ArrayList<>();
            operations.get(key).path("parameters").forEach(p -> params.add(p.path("name").asText()));
            assertThat(params).as("%s: %s", key, params).contains("page", "size", "sort");
        });
    }

    @Test
    @DisplayName("no empty schemas and every $ref resolves")
    void everyReferenceShouldResolve() {
        Set<String> missing = new LinkedHashSet<>();
        collectRefs(spec, missing);
        assertThat(missing).as("$ref points to a component that does not exist").isEmpty();
    }

    @Test
    @DisplayName("the 3 analytics DTOs use 3 separate Point schemas instead of a shared Item")
    void nestedItemSchemasShouldNotCollide() {
        JsonNode schemas = spec.path("components").path("schemas");
        assertThat(schemas.has("TimeseriesPoint")).as("ClickTimeseriesResponse.Item").isTrue();
        assertThat(schemas.has("HourlyClickPoint")).as("HourlyClicksResponse.Item").isTrue();
        assertThat(schemas.has("CountryClickPoint")).as("CountryClicksResponse.Item").isTrue();
        assertThat(schemas.has("Item")).as("a shared Item schema must not remain").isFalse();

        assertThat(schemas.path("HourlyClickPoint").path("properties").has("hour")).isTrue();
        assertThat(schemas.path("CountryClickPoint").path("properties").has("country")).isTrue();
        assertThat(schemas.path("TimeseriesPoint").path("properties").has("date")).isTrue();
    }

    @Test
    @DisplayName("ErrorResponse documents all 5 fields and errorCode lists every error code")
    void errorResponseShouldBeFullyDocumented() {
        JsonNode error = spec.path("components").path("schemas").path("ErrorResponse");
        assertThat(error.path("properties").properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("time", "status", "errorMessage", "errorCode", "path");

        JsonNode errorCode = error.path("properties").path("errorCode");
        assertThat(errorCode.toString())
                .as("errorCode must list RATE_LIMIT_EXCEEDED")
                .contains("RATE_LIMIT_EXCEEDED");
        assertThat(com.example.URLShortener.entity.enums.ErrorCode.values()).hasSize(16);
    }

    @Test
    @DisplayName("every request body and response body has a schema, no empty objects")
    void noEmptySchemas() {
        operations.forEach((key, operation) -> {
            JsonNode requestSchema = operation.path("requestBody").path("content")
                    .path(MediaType.APPLICATION_JSON_VALUE).path("schema");
            assertThat(requestSchema.isMissingNode() || isMeaningful(requestSchema))
                    .as("%s: request body schema is empty", key).isTrue();

            operation.path("responses").properties().forEach(response -> {
                JsonNode schema = response.getValue().path("content")
                        .path(MediaType.APPLICATION_JSON_VALUE).path("schema");
                if (!schema.isMissingNode()) {
                    assertThat(isMeaningful(schema))
                            .as("%s -> %s: response schema is empty", key, response.getKey()).isTrue();
                }
            });
        });
    }

    private static boolean isMeaningful(JsonNode schema) {
        return schema.has("$ref") || schema.path("properties").size() > 0
                || "array".equals(schema.path("type").asText());
    }

    private void collectRefs(JsonNode node, Set<String> missing) {
        if (node.isObject()) {
            JsonNode ref = node.path("$ref");
            if (ref.isTextual() && ref.asText().startsWith("#/components/")) {
                String[] parts = ref.asText().substring("#/components/".length()).split("/", 2);
                if (parts.length == 2 && !spec.path("components").path(parts[0]).has(parts[1])) {
                    missing.add(ref.asText());
                }
            }
            node.properties().forEach(entry -> collectRefs(entry.getValue(), missing));
        } else if (node.isArray()) {
            node.forEach(child -> collectRefs(child, missing));
        }
    }
}