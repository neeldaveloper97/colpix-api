package com.colpix.api.security;

import com.colpix.api.model.ApiClient;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

/**
 * Authentication object placed in the SecurityContext after a token is
 * validated. Carries the resolved {@link ApiClient} as the principal so
 * controllers can access the caller's collection without re-querying the
 * token store.
 */
public class AuthenticatedClient extends AbstractAuthenticationToken {

    private final ApiClient client;
    private final String tokenValue;

    public AuthenticatedClient(ApiClient client, String tokenValue) {
        super(List.of());
        this.client = client;
        this.tokenValue = tokenValue;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return tokenValue;
    }

    @Override
    public Object getPrincipal() {
        return client;
    }

    public ApiClient client() {
        return client;
    }
}
