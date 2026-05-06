package com.colpix.api.model;

import java.time.Instant;

/**
 * Server-side record of a token issued to a client. The {@code value} is the
 * opaque token string presented by callers; the cache uses it as the lookup key.
 */
public record IssuedToken(String value, String clientId, Instant issuedAt, Instant expiresAt) {

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
