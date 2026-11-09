package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.response.ApiResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProductVariantController {
    private final ProductVariantService productVariantService;

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariantToProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productVariantService.addVariantToProduct(productId, request);
        return new ResponseEntity<>(ApiResponse.success("Product variant added successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/products/{productId}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProductId(
            @PathVariable Long productId) {
        List<ProductVariantResponse> responses = productVariantService.getVariantsByProductId(productId);
        return new ResponseEntity<>(ApiResponse.success("Product variants fetched successfully", responses), HttpStatus.OK);
    }

    @GetMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(@PathVariable Long id) {
        ProductVariantResponse response = productVariantService.getVariantById(id);
        return new ResponseEntity<>(ApiResponse.success("Product variant fetched successfully", response), HttpStatus.OK);
    }

    @PutMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productVariantService.updateVariant(id, request);
        return new ResponseEntity<>(ApiResponse.success("Product variant updated successfully", response), HttpStatus.OK);
    }

    @DeleteMapping("/variants/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long id) {
        productVariantService.deleteVariant(id);
        return new ResponseEntity<>(ApiResponse.success("Product variant deleted successfully", null), HttpStatus.OK);
    }
}

