package com.colpix.api.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {
    public ProductNotFoundException(String productId) {
        super(HttpStatus.NOT_FOUND, "product_not_found",
                "Product '" + productId + "' was not found in your collection");
    }
}
