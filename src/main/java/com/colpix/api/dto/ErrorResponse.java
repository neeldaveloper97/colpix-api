package com.colpix.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error envelope. RFC-7807-inspired but kept simple.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public ErrorResponse(int status, String error, String code, String message, String path) {
        this(Instant.now(), status, error, code, message, path, null);
    }

    public ErrorResponse(int status, String error, String code, String message, String path, List<FieldError> fieldErrors) {
        this(Instant.now(), status, error, code, message, path, fieldErrors);
    }

    public record FieldError(String field, String message) {}
}
