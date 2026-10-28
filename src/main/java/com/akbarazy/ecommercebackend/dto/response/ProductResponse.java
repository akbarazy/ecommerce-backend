package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private List<ProductOptionResponse> options;
    private List<ProductVariantResponse> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        if (product == null) return null;
        List<ProductOptionResponse> optionResponses = product.getOptions() != null
            ? product.getOptions().stream().map(ProductOptionResponse::from).toList()
            : Collections.emptyList();

        List<ProductVariantResponse> variantResponses = product.getVariants() != null
            ? product.getVariants().stream().map(ProductVariantResponse::from).toList()
            : Collections.emptyList();

        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .options(optionResponses)
            .variants(variantResponses)
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}