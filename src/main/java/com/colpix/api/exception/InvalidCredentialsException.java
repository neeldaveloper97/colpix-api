package com.colpix.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid client_id or client_secret");
    }
}
