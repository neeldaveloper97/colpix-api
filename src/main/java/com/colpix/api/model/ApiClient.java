package com.colpix.api.model;

/**
 * An authenticated API client. Each client has its own logical product
 * collection in the backend; products created by the client are scoped to
 * its {@code collectionId}.
 */
public record ApiClient(String clientId, String collectionId) {}
