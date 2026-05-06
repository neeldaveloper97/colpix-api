package com.colpix.api.service;

import com.colpix.api.config.AppProperties;
import com.colpix.api.model.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRegistryTest {

    private ClientRegistry registry;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        AppProperties properties = new AppProperties(
                new AppProperties.Auth(Duration.ofMinutes(5)),
                new AppProperties.Backend("http://example.test", Duration.ofSeconds(1), Duration.ofSeconds(1)),
                List.of(
                        new AppProperties.ClientCredential("acme", encoder.encode("acme-secret"), "col-acme"),
                        new AppProperties.ClientCredential("contoso", encoder.encode("contoso-secret"), "col-contoso")));
        registry = new ClientRegistry(properties, encoder);
        registry.index();
    }

    @Test
    void authenticatesValidClient() {
        Optional<ApiClient> client = registry.authenticate("acme", "acme-secret");

        assertThat(client).isPresent().hasValueSatisfying(c -> {
            assertThat(c.clientId()).isEqualTo("acme");
            assertThat(c.collectionId()).isEqualTo("col-acme");
        });
    }

    @Test
    void rejectsWrongSecret() {
        assertThat(registry.authenticate("acme", "wrong")).isEmpty();
    }

    @Test
    void rejectsUnknownClient() {
        assertThat(registry.authenticate("nope", "any")).isEmpty();
    }

    @Test
    void findByIdReturnsClient() {
        assertThat(registry.findById("contoso"))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.collectionId()).isEqualTo("col-contoso"));
    }
}
