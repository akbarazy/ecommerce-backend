package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.ProductOption;
import com.akbarazy.ecommercebackend.entity.VariantOption;
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
public class ProductOptionResponse {
    private Long id;
    private Long productId;
    private String name;
    private List<String> values;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductOptionResponse from(ProductOption option) {
        if (option == null) return null;
        return ProductOptionResponse.builder()
            .id(option.getId())
            .productId(option.getProduct() != null ? option.getProduct().getId() : null)
            .name(option.getName())
            .values(option.getValues() != null
                ? option.getValues().stream().map(VariantOption::getValue).distinct().toList()
                : Collections.emptyList())
            .createdAt(option.getCreatedAt())
            .updatedAt(option.getUpdatedAt())
            .build();
    }
}