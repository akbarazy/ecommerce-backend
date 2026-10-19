package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.CartItem;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.Variant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    private String color;
    private String size;
    private BigDecimal price;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse from(CartItem cartItem) {
        if (cartItem == null) return null;
        Variant variant = cartItem.getVariant();
        Product product = cartItem.getProduct();
        return CartItemResponse.builder()
            .id(cartItem.getId())
            .variantId(variant != null ? variant.getId() : null)
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .sku(variant != null ? variant.getSku() : null)
            .color(variant != null ? variant.getColor() : null)
            .size(variant != null ? variant.getSize() : null)
            .price(variant != null ? variant.getPrice() : null)
            .imageUrl(variant != null ? variant.getImageUrl() : null)
            .quantity(cartItem.getQuantity())
            .subtotal(cartItem.getSubtotal())
            .build();
    }
}