package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.Variant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String color;
    private String size;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VariantResponse from(Variant variant) {
        if (variant == null) return null;
        return VariantResponse.builder()
            .id(variant.getId())
            .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
            .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
            .sku(variant.getSku())
            .color(variant.getColor())
            .size(variant.getSize())
            .price(variant.getPrice())
            .stock(variant.getStock())
            .imageUrl(variant.getImageUrl())
            .createdAt(variant.getCreatedAt())
            .updatedAt(variant.getUpdatedAt())
            .build();
    }
}