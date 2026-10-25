package com.akbarazy.ecommercebackend.repository;

import com.akbarazy.ecommercebackend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);
}