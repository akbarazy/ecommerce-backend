package com.akbarazy.ecommercebackend.repository;

import com.akbarazy.ecommercebackend.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    List<ProductOption> findByProductId(Long productId);
    Optional<ProductOption> findByProductIdAndName(Long productId, String name);
    boolean existsByProductIdAndName(Long productId, String name);
    void deleteByProductId(Long productId);
}