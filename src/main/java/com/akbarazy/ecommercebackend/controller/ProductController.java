package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.response.ApiResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.service.ProductService;
import com.akbarazy.ecommercebackend.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductVariantService productVariantService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        Page<ProductResponse> responses = productService.getAllProducts(search, categoryId, minPrice, maxPrice, pageable);
        return new ResponseEntity<>(ApiResponse.success("Products fetched successfully", responses), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return new ResponseEntity<>(ApiResponse.success("Product fetched successfully", response), HttpStatus.OK);
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getProductVariants(@PathVariable Long id) {
        List<ProductVariantResponse> responses = productVariantService.getVariantsByProductId(id);
        return new ResponseEntity<>(ApiResponse.success("Product variants fetched successfully", responses), HttpStatus.OK);
    }

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(@PathVariable Long variantId) {
        ProductVariantResponse response = productVariantService.getVariantById(variantId);
        return new ResponseEntity<>(ApiResponse.success("Product variant fetched successfully", response), HttpStatus.OK);
    }
}
