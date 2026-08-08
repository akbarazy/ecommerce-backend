package com.akbarazy.ecommercebackend.repository;

import com.akbarazy.ecommercebackend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
    
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    
    void deleteByUserId(Long userId);
}
