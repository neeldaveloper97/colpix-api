package com.colpix.api.exception;

import org.springframework.http.HttpStatus;

public class BackendUnavailableException extends ApiException {
    public BackendUnavailableException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "backend_unavailable", message, cause);
    }
}
