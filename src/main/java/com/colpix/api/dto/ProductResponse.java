package com.colpix.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "A product as returned by the API.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(

        @Schema(example = "ff80818193cbb35a0193cbb3aaaa0001")
        String id,

        @Schema(example = "Apple iPhone 15 Pro")
        String name,

        Map<String, Object> data,

        @Schema(description = "Server-side timestamp the response was assembled at.")
        Instant retrievedAt
) {}
