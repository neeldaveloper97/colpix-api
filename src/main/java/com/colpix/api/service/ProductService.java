package com.colpix.api.service;

import com.colpix.api.dto.ProductRequest;
import com.colpix.api.dto.ProductResponse;
import com.colpix.api.exception.ProductNotFoundException;
import com.colpix.api.model.ApiClient;
import com.colpix.api.model.BackendProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Application service for product CRUD. Calls the backend through
 * {@link BackendProductsClient} and scopes results to the caller's
 * collection through {@link ProductCollectionStore}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final BackendProductsClient backend;
    private final ProductCollectionStore collections;
    private final Clock clock;

    public List<ProductResponse> listForClient(ApiClient client) {
        Set<String> ownedIds = collections.snapshot(client.collectionId());
        if (ownedIds.isEmpty()) {
            return List.of();
        }
        Instant now = clock.instant();
        return backend.findByIds(ownedIds).stream()
                .map(p -> toResponse(p, now))
                .toList();
    }

    public ProductResponse getForClient(ApiClient client, String productId) {
        if (!collections.contains(client.collectionId(), productId)) {
            throw new ProductNotFoundException(productId);
        }
        BackendProduct product = backend.findById(productId);
        return toResponse(product, clock.instant());
    }

    public ProductResponse createForClient(ApiClient client, ProductRequest request) {
        BackendProduct created = backend.create(request.name(), request.data());
        collections.addToCollection(client.collectionId(), created.id());
        log.info("Created product id={} for collection={}", created.id(), client.collectionId());
        return toResponse(created, clock.instant());
    }

    public ProductResponse updateForClient(ApiClient client, String productId, ProductRequest request) {
        if (!collections.contains(client.collectionId(), productId)) {
            throw new ProductNotFoundException(productId);
        }
        BackendProduct updated = backend.update(productId, request.name(), request.data());
        log.info("Updated product id={} for collection={}", productId, client.collectionId());
        return toResponse(updated, clock.instant());
    }

    private static ProductResponse toResponse(BackendProduct p, Instant retrievedAt) {
        return new ProductResponse(p.id(), p.name(), p.data(), retrievedAt);
    }
}
