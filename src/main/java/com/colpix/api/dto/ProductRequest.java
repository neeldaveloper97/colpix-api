package com.colpix.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Payload for creating or updating a product.")
public record ProductRequest(

        @Schema(example = "Apple iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "Free-form attributes for the product (price, color, capacity, ...).",
                example = "{\"color\":\"Natural Titanium\",\"price\":1199.00,\"capacity\":\"256 GB\"}")
        Map<String, Object> data
) {}
