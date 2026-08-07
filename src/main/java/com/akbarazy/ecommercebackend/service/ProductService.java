package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.CreateProductRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse getProductById(Long id);
    Page<ProductResponse> getAllProducts(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    ProductResponse updateProduct(Long id, CreateProductRequest request);
    void deleteProduct(Long id);
}
