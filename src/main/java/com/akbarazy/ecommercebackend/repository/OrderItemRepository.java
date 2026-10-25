package com.akbarazy.ecommercebackend.repository;

import com.akbarazy.ecommercebackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByVariantId(Long variantId);
    boolean existsByVariantId(Long variantId);
}