package com.colpix.api.security;

import com.colpix.api.dto.ErrorResponse;
import com.colpix.api.model.ApiClient;
import com.colpix.api.service.ClientRegistry;
import com.colpix.api.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Validates a bearer token on each request, populating the SecurityContext
 * when valid. Public endpoints are excluded from this filter via
 * {@link #shouldNotFilter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final ClientRegistry clientRegistry;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        return path.equals("/api/v1/auth/login")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Optional<String> token = extractBearerToken(request);
        if (token.isEmpty()) {
            writeError(request, response, HttpStatus.UNAUTHORIZED, "missing_token",
                    "Authorization header with Bearer token is required");
            return;
        }

        var resolved = tokenService.resolve(token.get());
        if (resolved.isEmpty()) {
            writeError(request, response, HttpStatus.UNAUTHORIZED, "invalid_or_expired_token",
                    "Token is invalid or has expired");
            return;
        }

        Optional<ApiClient> client = clientRegistry.findById(resolved.get().clientId());
        if (client.isEmpty()) {
            // Client was removed since the token was issued.
            tokenService.revoke(token.get());
            writeError(request, response, HttpStatus.UNAUTHORIZED, "invalid_or_expired_token",
                    "Token is invalid or has expired");
            return;
        }

        AuthenticatedClient authentication = new AuthenticatedClient(client.get(), token.get());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response,
                            HttpStatus status, String code, String message) throws IOException {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
