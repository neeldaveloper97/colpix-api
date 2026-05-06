package com.colpix.api.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which backend product ids belong to each client's collection.
 *
 * <p>The challenge maps each {@code client_id} to one Collection resource in
 * the backend, but restful-api.dev has no concept of collections — every
 * object lives in a single global namespace. We therefore keep the
 * id-to-collection mapping locally so we can scope reads, updates, and
 * "list" responses to the calling client only.
 *
 * <p>Storage is in-memory and thread-safe. Replace with a database-backed
 * implementation for production; the contract — add, contains, snapshot —
 * stays the same.
 */
@Component
public class ProductCollectionStore {

    private final ConcurrentHashMap<String, Set<String>> collections = new ConcurrentHashMap<>();

    /** Add a product id to the given collection. Idempotent. */
    public void addToCollection(String collectionId, String productId) {
        collections.computeIfAbsent(collectionId, k -> Collections.synchronizedSet(new LinkedHashSet<>()))
                .add(productId);
    }

    /** True iff the given product id is in the named collection. */
    public boolean contains(String collectionId, String productId) {
        Set<String> ids = collections.get(collectionId);
        return ids != null && ids.contains(productId);
    }

    /** Returns an immutable snapshot of the collection's product ids. */
    public Set<String> snapshot(String collectionId) {
        Set<String> ids = collections.get(collectionId);
        if (ids == null) {
            return Set.of();
        }
        synchronized (ids) {
            return Set.copyOf(ids);
        }
    }
}
