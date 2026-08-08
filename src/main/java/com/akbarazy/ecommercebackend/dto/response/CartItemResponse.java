package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.CartItem;
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
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse fromEntity(CartItem cartItem) {
        if (cartItem == null) return null;
        
        return CartItemResponse.builder()
            .id(cartItem.getId())
            .productId(cartItem.getProduct() != null ? cartItem.getProduct().getId() : null)
            .productName(cartItem.getProduct() != null ? cartItem.getProduct().getName() : null)
            .productPrice(cartItem.getProduct() != null ? cartItem.getProduct().getPrice() : null)
            .productImageUrl(cartItem.getProduct() != null ? cartItem.getProduct().getImageUrl() : null)
            .quantity(cartItem.getQuantity())
            .subtotal(cartItem.getSubtotal())
            .build();
    }
}
