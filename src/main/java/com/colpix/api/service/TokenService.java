package com.colpix.api.service;

import com.colpix.api.config.AppProperties;
import com.colpix.api.model.IssuedToken;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and validates opaque bearer tokens. Tokens are 32 random bytes
 * encoded as URL-safe base64 (~43 chars). Storage is an in-memory Caffeine
 * cache that expires entries at exactly {@code expiresAt}, so memory does
 * not grow unbounded as tokens age out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final AppProperties properties;
    private final Clock clock;

    private Cache<String, IssuedToken> tokens;

    @PostConstruct
    void initStore() {
        this.tokens = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, IssuedToken>() {
                    @Override
                    public long expireAfterCreate(String key, IssuedToken token, long currentTimeNanos) {
                        return nanosUntil(token.expiresAt());
                    }

                    @Override
                    public long expireAfterUpdate(String key, IssuedToken token,
                                                  long currentTimeNanos, long currentDurationNanos) {
                        return nanosUntil(token.expiresAt());
                    }

                    @Override
                    public long expireAfterRead(String key, IssuedToken token,
                                                long currentTimeNanos, long currentDurationNanos) {
                        return currentDurationNanos;
                    }
                })
                .build();
    }

    /** Issue a new token for the given client. */
    public IssuedToken issue(String clientId) {
        Instant now = clock.instant();
        Duration ttl = properties.auth().tokenTtl();
        Instant expiresAt = now.plus(ttl);

        IssuedToken token = new IssuedToken(generateOpaqueToken(), clientId, now, expiresAt);
        tokens.put(token.value(), token);
        log.debug("Issued token for client={} ttlSeconds={}", clientId, ttl.toSeconds());
        return token;
    }

    /** Resolve a token, returning empty if unknown or expired. */
    public Optional<IssuedToken> resolve(String tokenValue) {
        IssuedToken token = tokens.getIfPresent(tokenValue);
        if (token == null) {
            return Optional.empty();
        }
        if (token.isExpired(clock.instant())) {
            tokens.invalidate(tokenValue);
            return Optional.empty();
        }
        return Optional.of(token);
    }

    public void revoke(String tokenValue) {
        tokens.invalidate(tokenValue);
    }

    /** Visible for tests / metrics. */
    public long activeTokenCount() {
        tokens.cleanUp();
        return tokens.estimatedSize();
    }

    private long nanosUntil(Instant expiresAt) {
        long nanos = Duration.between(clock.instant(), expiresAt).toNanos();
        return Math.max(nanos, 0L);
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
