package com.colpix.api.service;

import com.colpix.api.config.AppProperties;
import com.colpix.api.model.IssuedToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private MutableClock clock;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-05-01T10:00:00Z"));
        AppProperties properties = new AppProperties(
                new AppProperties.Auth(Duration.ofMinutes(5)),
                new AppProperties.Backend("http://example.test", Duration.ofSeconds(1), Duration.ofSeconds(1)),
                List.of());
        tokenService = new TokenService(properties, clock);
        tokenService.initStore();
    }

    @Test
    void issuesTokenWithExpectedTtl() {
        IssuedToken token = tokenService.issue("acme");

        assertThat(token.value()).isNotBlank().hasSizeGreaterThan(20);
        assertThat(token.clientId()).isEqualTo("acme");
        assertThat(token.issuedAt()).isEqualTo(clock.instant());
        assertThat(Duration.between(token.issuedAt(), token.expiresAt())).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void resolvesValidToken() {
        IssuedToken token = tokenService.issue("acme");

        Optional<IssuedToken> resolved = tokenService.resolve(token.value());

        assertThat(resolved).isPresent().hasValueSatisfying(t -> {
            assertThat(t.clientId()).isEqualTo("acme");
            assertThat(t.value()).isEqualTo(token.value());
        });
    }

    @Test
    void resolveReturnsEmptyForUnknownToken() {
        assertThat(tokenService.resolve("not-a-real-token")).isEmpty();
    }

    @Test
    void resolveReturnsEmptyForExpiredToken() {
        IssuedToken token = tokenService.issue("acme");

        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        assertThat(tokenService.resolve(token.value())).isEmpty();
    }

    @Test
    void revokeRemovesToken() {
        IssuedToken token = tokenService.issue("acme");

        tokenService.revoke(token.value());

        assertThat(tokenService.resolve(token.value())).isEmpty();
    }

    @Test
    void issuesUniqueTokens() {
        IssuedToken first = tokenService.issue("acme");
        IssuedToken second = tokenService.issue("acme");

        assertThat(first.value()).isNotEqualTo(second.value());
    }

    /** Clock that tests can advance manually. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant initial) { this.now = initial; }

        void advance(Duration delta) { this.now = this.now.plus(delta); }

        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }
}
