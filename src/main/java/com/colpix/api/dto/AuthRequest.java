package com.colpix.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials presented to /auth/login.")
public record AuthRequest(

        @Schema(example = "acme", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("client_id")
        @NotBlank
        String clientId,

        @Schema(example = "acme-secret", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("client_secret")
        @NotBlank
        String clientSecret
) {}
