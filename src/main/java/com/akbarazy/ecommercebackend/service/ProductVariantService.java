package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {
    ProductVariantResponse addVariantToProduct(Long productId, ProductVariantRequest request);
    List<ProductVariantResponse> getVariantsByProductId(Long productId);
    ProductVariantResponse getVariantById(Long id);
    ProductVariantResponse updateVariant(Long id, ProductVariantRequest request);
    void deleteVariant(Long id);
}

