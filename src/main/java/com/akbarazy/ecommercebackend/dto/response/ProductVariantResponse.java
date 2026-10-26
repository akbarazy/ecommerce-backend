package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal price;
    private Integer stock;
    private BigDecimal weight;
    private String imageUrl;
    private List<VariantOptionResponse> optionValues;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductVariantResponse from(ProductVariant variant) {
        if (variant == null) return null;
        return ProductVariantResponse.builder()
            .id(variant.getId())
            .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
            .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
            .sku(variant.getSku())
            .price(variant.getPrice())
            .stock(variant.getStock())
            .weight(variant.getWeight())
            .imageUrl(variant.getImageUrl())
            .optionValues(variant.getValues() != null
                ? variant.getValues().stream().map(VariantOptionResponse::from).toList()
                : Collections.emptyList())
            .createdAt(variant.getCreatedAt())
            .updatedAt(variant.getUpdatedAt())
            .build();
    }

    public List<VariantOptionResponse> getOptions() {
        return optionValues;
    }
}