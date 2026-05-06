package com.colpix.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ColpixApiIntegrationTest {

    private static final WireMockServer BACKEND = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        BACKEND.start();
    }

    @AfterAll
    static void stopWireMock() {
        BACKEND.stop();
    }

    @DynamicPropertySource
    static void backendProps(DynamicPropertyRegistry registry) {
        if (!BACKEND.isRunning()) {
            BACKEND.start();
        }
        registry.add("colpix.backend.base-url", () -> "http://localhost:" + BACKEND.port());
    }

    @Test
    @Order(1)
    void loginRejectsBadCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client_id":"acme","client_secret":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    @Order(2)
    void loginRejectsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client_id":"","client_secret":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    @Order(3)
    void protectedEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("missing_token"));
    }

    @Test
    @Order(4)
    void protectedEndpointRejectsBogusToken() throws Exception {
        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_or_expired_token"));
    }

    @Test
    @Order(5)
    void fullCrudFlow() throws Exception {
        BACKEND.resetAll();
        String token = login("acme", "acme-secret");

        // Initially the collection is empty so the service should not even
        // call the backend; the response is an empty list directly.
        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Create → backend assigns id
        BACKEND.stubFor(WireMock.post(WireMock.urlEqualTo("/objects"))
                .willReturn(WireMock.okJson("""
                        {"id":"new-1","name":"iPhone 15","data":{"color":"Titanium","price":1199}}
                        """)));

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"iPhone 15","data":{"color":"Titanium","price":1199}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value("new-1"));

        // List queries the backend by the owned id only.
        BACKEND.stubFor(WireMock.get(WireMock.urlPathEqualTo("/objects"))
                .withQueryParam("id", WireMock.equalTo("new-1"))
                .willReturn(WireMock.okJson("""
                        [{"id":"new-1","name":"iPhone 15","data":{"color":"Titanium","price":1199}}]
                        """)));

        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("new-1"));

        // Get owned product
        BACKEND.stubFor(WireMock.get(WireMock.urlEqualTo("/objects/new-1"))
                .willReturn(WireMock.okJson("""
                        {"id":"new-1","name":"iPhone 15","data":{"color":"Titanium"}}
                        """)));

        mockMvc.perform(get("/api/v1/products/new-1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-1"));

        // Get unknown product → 404 (not in collection)
        mockMvc.perform(get("/api/v1/products/backend-1").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product_not_found"));

        // Update owned product
        BACKEND.stubFor(WireMock.put(WireMock.urlEqualTo("/objects/new-1"))
                .willReturn(WireMock.okJson("""
                        {"id":"new-1","name":"iPhone 15 Pro","data":{"color":"Titanium","price":1299}}
                        """)));

        mockMvc.perform(put("/api/v1/products/new-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"iPhone 15 Pro","data":{"color":"Titanium","price":1299}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone 15 Pro"));

        // Update fails for product not in collection
        mockMvc.perform(put("/api/v1/products/backend-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"x"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product_not_found"));
    }

    @Test
    @Order(6)
    void crossClientIsolation() throws Exception {
        BACKEND.resetAll();
        BACKEND.stubFor(WireMock.post(WireMock.urlEqualTo("/objects"))
                .willReturn(WireMock.okJson("""
                        {"id":"acme-only","name":"Macbook","data":null}
                        """)));

        String acmeToken = login("acme", "acme-secret");
        String contosoToken = login("contoso", "contoso-secret");

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + acmeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Macbook"}
                                """))
                .andExpect(status().isCreated());

        // contoso must not see acme's product
        mockMvc.perform(get("/api/v1/products/acme-only")
                        .header("Authorization", "Bearer " + contosoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product_not_found"));

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + contosoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(7)
    void loginResponseExposesExpiry() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client_id":"acme","client_secret":"acme-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(300))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("issued_at").asText()).isNotBlank();
        assertThat(body.get("expires_at").asText()).isNotBlank();
    }

    private String login(String clientId, String clientSecret) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "client_id", clientId,
                                "client_secret", clientSecret))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("access_token").asText();
    }
}
