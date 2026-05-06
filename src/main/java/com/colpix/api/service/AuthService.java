package com.colpix.api.service;

import com.colpix.api.dto.AuthRequest;
import com.colpix.api.dto.AuthResponse;
import com.colpix.api.exception.InvalidCredentialsException;
import com.colpix.api.model.ApiClient;
import com.colpix.api.model.IssuedToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRegistry clientRegistry;
    private final TokenService tokenService;

    public AuthResponse login(AuthRequest request) {
        ApiClient client = clientRegistry.authenticate(request.clientId(), request.clientSecret())
                .orElseThrow(() -> {
                    log.info("Authentication failed for clientId={}", request.clientId());
                    return new InvalidCredentialsException();
                });
        IssuedToken token = tokenService.issue(client.clientId());
        log.info("Issued token for clientId={} expiresAt={}", client.clientId(), token.expiresAt());
        return AuthResponse.bearer(token.value(), token.issuedAt(), token.expiresAt());
    }
}
