package com.akbarazy.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "variant_id"}
    )
)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal getSubtotal() {
        if (variant == null || variant.getPrice() == null || quantity == null) return BigDecimal.ZERO;
        return variant.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() {
        return variant != null ? variant.getProduct() : null;
    }

    public ProductVariant getProductVariant() {
        return this.variant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.variant = productVariant;
    }

    public static class CartItemBuilder {
        public CartItemBuilder productVariant(ProductVariant productVariant) {
            this.variant = productVariant;
            return this;
        }
    }
}