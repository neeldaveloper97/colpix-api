package com.colpix.api.service;

import com.colpix.api.config.AppProperties;
import com.colpix.api.model.ApiClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Holds the configured set of API clients and verifies their secrets.
 *
 * <p>Indexed in-memory at startup; lookups are O(1). For production this
 * would be backed by a persistent store, but the contract — id-based lookup
 * plus secret verification — would be unchanged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRegistry {

    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;

    private Map<String, AppProperties.ClientCredential> clientsById = Map.of();

    @PostConstruct
    void index() {
        clientsById = properties.clients().stream()
                .collect(Collectors.toUnmodifiableMap(
                        AppProperties.ClientCredential::clientId,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate clientId in configuration: " + a.clientId());
                        }));
        log.info("Loaded {} API client(s)", clientsById.size());
    }

    /**
     * Verify the secret and return the corresponding client. Returns empty
     * for any failure — unknown id or wrong secret — so callers can return
     * a single, opaque "invalid credentials" error.
     */
    public Optional<ApiClient> authenticate(String clientId, String clientSecret) {
        AppProperties.ClientCredential credential = clientsById.get(clientId);
        if (credential == null) {
            // Hash a dummy value to make timing roughly equal between unknown
            // and known client ids.
            passwordEncoder.matches(clientSecret, "$2a$10$0000000000000000000000.0000000000000000000000000000000");
            return Optional.empty();
        }
        if (!passwordEncoder.matches(clientSecret, credential.clientSecretHash())) {
            return Optional.empty();
        }
        return Optional.of(new ApiClient(credential.clientId(), credential.collectionId()));
    }

    public Optional<ApiClient> findById(String clientId) {
        return Optional.ofNullable(clientsById.get(clientId))
                .map(c -> new ApiClient(c.clientId(), c.collectionId()));
    }

    /** Test seam to swap clients at runtime. */
    void replaceForTests(Map<String, AppProperties.ClientCredential> map) {
        this.clientsById = new HashMap<>(map);
    }
}
