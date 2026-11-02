package com.akbarazy.ecommercebackend.repository;

import com.akbarazy.ecommercebackend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :quantity WHERE pv.id = :id AND pv.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock + :quantity WHERE pv.id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}