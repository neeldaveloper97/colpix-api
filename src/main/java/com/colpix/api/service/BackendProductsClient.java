package com.colpix.api.service;

import com.colpix.api.exception.BackendUnavailableException;
import com.colpix.api.exception.ProductNotFoundException;
import com.colpix.api.model.BackendProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over the restful-api.dev /objects endpoints. Translates
 * HTTP outcomes into domain exceptions so upstream code never deals with
 * raw RestClient errors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackendProductsClient {

    private static final String OBJECTS_PATH = "/objects";
    private static final String OBJECT_BY_ID_PATH = "/objects/{id}";
    private static final ParameterizedTypeReference<List<BackendProduct>> PRODUCT_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient backendRestClient;

    public List<BackendProduct> findAll() {
        try {
            List<BackendProduct> body = backendRestClient.get()
                    .uri(OBJECTS_PATH)
                    .retrieve()
                    .body(PRODUCT_LIST);
            return body == null ? List.of() : body;
        } catch (RestClientResponseException ex) {
            throw mapResponseError("Failed to list products", ex);
        } catch (ResourceAccessException ex) {
            throw new BackendUnavailableException("Backend service is unreachable", ex);
        }
    }

    /**
     * Bulk lookup by id. Uses the {@code GET /objects?id=...&id=...} form
     * supported by restful-api.dev so user-created objects (which the
     * unfiltered {@code GET /objects} omits) are returned. Empty input
     * short-circuits to an empty list to avoid an unfiltered fetch.
     */
    public List<BackendProduct> findByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        try {
            List<BackendProduct> body = backendRestClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path(OBJECTS_PATH);
                        for (String id : ids) {
                            b.queryParam("id", id);
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(PRODUCT_LIST);
            return body == null ? List.of() : body;
        } catch (RestClientResponseException ex) {
            throw mapResponseError("Failed to list products by id", ex);
        } catch (ResourceAccessException ex) {
            throw new BackendUnavailableException("Backend service is unreachable", ex);
        }
    }

    public BackendProduct findById(String id) {
        try {
            return backendRestClient.get()
                    .uri(OBJECT_BY_ID_PATH, id)
                    .retrieve()
                    .body(BackendProduct.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ProductNotFoundException(id);
            }
            throw mapResponseError("Failed to get product", ex);
        } catch (ResourceAccessException ex) {
            throw new BackendUnavailableException("Backend service is unreachable", ex);
        }
    }

    public BackendProduct create(String name, Map<String, Object> data) {
        try {
            return backendRestClient.post()
                    .uri(OBJECTS_PATH)
                    .body(buildBody(name, data))
                    .retrieve()
                    .body(BackendProduct.class);
        } catch (RestClientResponseException ex) {
            throw mapResponseError("Failed to create product", ex);
        } catch (ResourceAccessException ex) {
            throw new BackendUnavailableException("Backend service is unreachable", ex);
        }
    }

    public BackendProduct update(String id, String name, Map<String, Object> data) {
        try {
            return backendRestClient.put()
                    .uri(OBJECT_BY_ID_PATH, id)
                    .body(buildBody(name, data))
                    .retrieve()
                    .body(BackendProduct.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ProductNotFoundException(id);
            }
            throw mapResponseError("Failed to update product", ex);
        } catch (ResourceAccessException ex) {
            throw new BackendUnavailableException("Backend service is unreachable", ex);
        }
    }

    private static Map<String, Object> buildBody(String name, Map<String, Object> data) {
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        if (data != null) {
            body.put("data", data);
        }
        return body;
    }

    private static BackendUnavailableException mapResponseError(String operation,
                                                                RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        log.warn("{}: backend returned {} {}", operation, status.value(), ex.getResponseBodyAsString());
        return new BackendUnavailableException(operation + ": backend returned " + status.value(), ex);
    }
}
