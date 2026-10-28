package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.CartItem;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal price;
    private String imageUrl;
    private List<VariantOptionResponse> variantOptions;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse from(CartItem cartItem) {
        if (cartItem == null) return null;
        ProductVariant variant = cartItem.getVariant();
        Product product = cartItem.getProduct();

        List<VariantOptionResponse> variantOptions = (variant != null && variant.getValues() != null)
            ? variant.getValues().stream().map(VariantOptionResponse::from).toList()
            : Collections.emptyList();

        return CartItemResponse.builder()
            .id(cartItem.getId())
            .variantId(variant != null ? variant.getId() : null)
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .sku(variant != null ? variant.getSku() : null)
            .price(variant != null ? variant.getPrice() : null)
            .imageUrl(variant != null ? variant.getImageUrl() : null)
            .variantOptions(variantOptions)
            .quantity(cartItem.getQuantity())
            .subtotal(cartItem.getSubtotal())
            .build();
    }

    public List<VariantOptionResponse> getVariantOptions() {
        return variantOptions;
    }
}