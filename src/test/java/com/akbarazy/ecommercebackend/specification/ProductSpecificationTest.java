package com.akbarazy.ecommercebackend.specification;

import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ProductSpecificationTest {

    @Mock
    private Root<Product> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Join<Product, ProductVariant> variantJoin;

    @Mock
    private Path<Object> categoryPath;

    @Mock
    private Path<Object> categoryIdPath;

    @Mock
    private Path<Object> namePath;

    @Mock
    private Path<Object> descPath;

    @Mock
    private Path<Object> skuPath;

    @Mock
    private Path<Object> pricePath;

    @Mock
    private Predicate mockPredicate;

    @Mock
    private Expression<String> mockLowerExp;

    @Mock
    private Expression<String> mockCoalesceExp;

    @BeforeEach
    void setUp() {
        lenient().when(root.getJoins()).thenReturn(Collections.emptySet());
        lenient().doReturn(variantJoin).when(root).join(eq("variants"), eq(JoinType.LEFT));
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(mockPredicate);
        lenient().when(cb.or(any(Predicate[].class))).thenReturn(mockPredicate);
        lenient().when(cb.conjunction()).thenReturn(mockPredicate);
    }

    @Test
    @DisplayName("filterProducts: Sets distinct and joins variants for price filtering")
    void filterProducts_PriceFilter_Success() {
        BigDecimal minPrice = new BigDecimal("100000.00");
        BigDecimal maxPrice = new BigDecimal("300000.00");

        when(variantJoin.get("price")).thenReturn((Path) pricePath);
        when(cb.greaterThanOrEqualTo(any(Expression.class), eq(minPrice))).thenReturn(mockPredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), eq(maxPrice))).thenReturn(mockPredicate);

        Specification<Product> spec = ProductSpecification.filterProducts(null, null, minPrice, maxPrice);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(query, times(1)).distinct(true);
        verify(root, times(1)).join(eq("variants"), eq(JoinType.LEFT));
        verify(cb, times(1)).greaterThanOrEqualTo(any(Expression.class), eq(minPrice));
        verify(cb, times(1)).lessThanOrEqualTo(any(Expression.class), eq(maxPrice));
    }

    @Test
    @DisplayName("filterProducts: Searches by name, description, and variant SKU with distinct")
    void filterProducts_Search_Success() {
        String search = "Sneakers";

        when(root.get("name")).thenReturn((Path) namePath);
        when(root.get("description")).thenReturn((Path) descPath);
        when(variantJoin.get("sku")).thenReturn((Path) skuPath);
        when(cb.lower(any())).thenReturn(mockLowerExp);
        doReturn(mockCoalesceExp).when(cb).coalesce(eq(descPath), eq(""));
        when(cb.like(any(Expression.class), anyString())).thenReturn(mockPredicate);

        Specification<Product> spec = ProductSpecification.filterProducts(search, null, null, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(query, times(1)).distinct(true);
        verify(cb, times(1)).or(any(Predicate.class), any(Predicate.class), any(Predicate.class));
    }

    @Test
    @DisplayName("filterProducts: Filters by categoryId")
    void filterProducts_CategoryFilter_Success() {
        Long categoryId = 5L;

        when(root.get("category")).thenReturn((Path) categoryPath);
        when(categoryPath.get("id")).thenReturn((Path) categoryIdPath);
        when(cb.equal(categoryIdPath, categoryId)).thenReturn(mockPredicate);

        Specification<Product> spec = ProductSpecification.filterProducts(null, categoryId, null, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, times(1)).equal(categoryIdPath, categoryId);
    }

    @Test
    @DisplayName("filterProducts: Dynamic attribute filter creates subquery and distinct")
    void filterProducts_WithAttributes_Success() {
        Map<String, String> attributes = Map.of("Size", "42");

        Subquery<Long> variantSubquery = mock(Subquery.class);
        Root<ProductVariant> pvRoot = mock(Root.class);
        Subquery<Long> voSubquery = mock(Subquery.class);
        Root voRoot = mock(Root.class);
        Join poJoin = mock(Join.class);

        when(query.subquery(Long.class)).thenReturn(variantSubquery, voSubquery);
        when(variantSubquery.from(ProductVariant.class)).thenReturn(pvRoot);
        when(voSubquery.from(any(Class.class))).thenReturn(voRoot);
        when(voRoot.join("productOption")).thenReturn(poJoin);

        when(variantSubquery.select(any())).thenReturn(variantSubquery);
        when(variantSubquery.where(any(Predicate.class))).thenReturn(variantSubquery);
        when(voSubquery.select(any())).thenReturn(voSubquery);
        when(voSubquery.where(any(Predicate.class))).thenReturn(voSubquery);
        doReturn(mockPredicate).when(cb).exists(any(Subquery.class));

        Specification<Product> spec = ProductSpecification.filterProducts(null, null, null, null, attributes);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(query, times(1)).distinct(true);
        verify(cb, atLeastOnce()).exists(any(Subquery.class));
    }

    @Test
    @DisplayName("hasPriceBetween: Composable specification filters range")
    void hasPriceBetween_Success() {
        BigDecimal min = new BigDecimal("50000");
        BigDecimal max = new BigDecimal("150000");

        when(variantJoin.get("price")).thenReturn((Path) pricePath);
        when(cb.greaterThanOrEqualTo(any(Expression.class), eq(min))).thenReturn(mockPredicate);
        when(cb.lessThanOrEqualTo(any(Expression.class), eq(max))).thenReturn(mockPredicate);

        Specification<Product> spec = ProductSpecification.hasPriceBetween(min, max);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(query, times(1)).distinct(true);
        verify(cb, times(1)).greaterThanOrEqualTo(any(Expression.class), eq(min));
        verify(cb, times(1)).lessThanOrEqualTo(any(Expression.class), eq(max));
    }
}
