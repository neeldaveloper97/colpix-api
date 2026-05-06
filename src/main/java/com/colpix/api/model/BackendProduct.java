package com.colpix.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Wire model for restful-api.dev /objects responses. Field names match the
 * upstream JSON. Unknown fields are tolerated so the backend can evolve.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BackendProduct(String id, String name, Map<String, Object> data) {}
