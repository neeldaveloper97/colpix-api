package com.colpix.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Externalised configuration for the Colpix API.
 *
 * <p>Bound from the {@code colpix} prefix in {@code application.yml}. Validated
 * at startup so misconfiguration fails fast.
 */
@Validated
@ConfigurationProperties(prefix = "colpix")
public record AppProperties(
        @NotNull @Valid Auth auth,
        @NotNull @Valid Backend backend,
        @NotNull @Valid List<@Valid ClientCredential> clients
) {

    public record Auth(
            @NotNull Duration tokenTtl
    ) {}

    public record Backend(
            @NotBlank String baseUrl,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout
    ) {}

    /**
     * A client allowed to call this API. The {@code clientSecretHash} is a
     * BCrypt hash of the secret. The {@code collectionId} is a logical
     * identifier for the client's product collection in the backend.
     */
    public record ClientCredential(
            @NotBlank String clientId,
            @NotBlank String clientSecretHash,
            @NotBlank String collectionId
    ) {}
}
