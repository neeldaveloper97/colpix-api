package com.colpix.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Bearer token issued by /auth/login. Use it as 'Authorization: Bearer <access_token>'.")
public record AuthResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @Schema(description = "Token lifetime in seconds.")
        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("issued_at")
        Instant issuedAt,

        @JsonProperty("expires_at")
        Instant expiresAt
) {

    public static AuthResponse bearer(String token, Instant issuedAt, Instant expiresAt) {
        return new AuthResponse(
                token,
                "Bearer",
                java.time.Duration.between(issuedAt, expiresAt).toSeconds(),
                issuedAt,
                expiresAt);
    }
}
