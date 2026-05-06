package com.colpix.api.controller;

import com.colpix.api.dto.ErrorResponse;
import com.colpix.api.dto.ProductRequest;
import com.colpix.api.dto.ProductResponse;
import com.colpix.api.security.AuthenticatedClient;
import com.colpix.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "List all products in the caller's collection.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "List of products"))
    @GetMapping
    public List<ProductResponse> list(AuthenticatedClient principal) {
        return productService.listForClient(principal.client());
    }

    @Operation(summary = "Get a product by id from the caller's collection.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ProductResponse getById(AuthenticatedClient principal, @PathVariable String id) {
        return productService.getForClient(principal.client(), id);
    }

    @Operation(summary = "Create a product in the caller's collection.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(AuthenticatedClient principal,
                                                  @Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createForClient(principal.client(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
    }

    @Operation(summary = "Update a product in the caller's collection.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ProductResponse update(AuthenticatedClient principal,
                                  @PathVariable String id,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.updateForClient(principal.client(), id, request);
    }
}
