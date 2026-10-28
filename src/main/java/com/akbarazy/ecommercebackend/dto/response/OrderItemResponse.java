package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String variantAttributes;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem orderItem) {
        if (orderItem == null) return null;
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .variantId(orderItem.getVariant() != null ? orderItem.getVariant().getId() : null)
                .productId(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null)
                .productName(orderItem.getProductName() != null 
                    ? orderItem.getProductName() 
                    : (orderItem.getProduct() != null ? orderItem.getProduct().getName() : null))
                .variantAttributes(orderItem.getVariantAttributes())
                .quantity(orderItem.getQuantity())
                .priceAtPurchase(orderItem.getPriceAtPurchase())
                .subtotal(orderItem.getSubtotal())
                .build();
    }
}