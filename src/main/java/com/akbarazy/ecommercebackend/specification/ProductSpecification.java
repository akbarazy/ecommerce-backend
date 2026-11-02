package com.akbarazy.ecommercebackend.specification;

import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.ProductOption;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import com.akbarazy.ecommercebackend.entity.VariantOption;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductSpecification {

    /**
     * Filter produk berdasarkan kata kunci pencarian, kategori, dan rentang harga (minPrice, maxPrice).
     * Menggunakan join ke ProductVariant dengan distinct untuk filter harga dan pencarian SKU.
     */
    public static Specification<Product> filterProducts(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return filterProducts(search, categoryId, minPrice, maxPrice, (Map<String, String>) null);
    }

    /**
     * Filter produk berdasarkan kata kunci pencarian, kategori, rentang harga, dan opsi atribut tunggal.
     */
    public static Specification<Product> filterProducts(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String optionName,
            String optionValue
    ) {
        Map<String, String> attributes = null;
        if ((optionName != null && !optionName.trim().isEmpty()) || (optionValue != null && !optionValue.trim().isEmpty())) {
            attributes = new HashMap<>();
            attributes.put(optionName, optionValue);
        }
        return filterProducts(search, categoryId, minPrice, maxPrice, attributes);
    }

    /**
     * Filter produk lengkap dengan dukungan filter dinamis atribut/opsi (Map nama-nilai opsi).
     */
    public static Specification<Product> filterProducts(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Map<String, String> attributes
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                query.distinct(true);
            }

            Join<Product, ProductVariant> variantJoin = getOrCreateVariantJoin(root);

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        searchPattern
                );
                Predicate descMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
                        searchPattern
                );
                Predicate skuMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(variantJoin.get("sku")),
                        searchPattern
                );
                predicates.add(criteriaBuilder.or(nameMatch, descMatch, skuMatch));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(variantJoin.get("price"), maxPrice));
            }

            if (attributes != null && !attributes.isEmpty()) {
                Predicate attributePredicate = buildAttributesPredicate(root, query, criteriaBuilder, attributes);
                if (attributePredicate != null) {
                    predicates.add(attributePredicate);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Membangun predicate untuk filter dinamis berdasarkan atribut/opsi varian.
     * Menggunakan subquery EXISTS untuk mencocokkan ProductVariant yang memiliki kombinasi opsi yang diminta.
     */
    private static Predicate buildAttributesPredicate(
            Root<Product> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            Map<String, String> attributes
    ) {
        if (query == null) {
            return null;
        }

        List<Map.Entry<String, String>> validEntries = attributes.entrySet().stream()
                .filter(e -> (e.getKey() != null && !e.getKey().trim().isEmpty())
                          || (e.getValue() != null && !e.getValue().trim().isEmpty()))
                .toList();

        if (validEntries.isEmpty()) {
            return null;
        }

        Subquery<Long> variantSubquery = query.subquery(Long.class);
        Root<ProductVariant> pvRoot = variantSubquery.from(ProductVariant.class);

        List<Predicate> pvPredicates = new ArrayList<>();
        pvPredicates.add(criteriaBuilder.equal(pvRoot.get("product"), root));

        for (Map.Entry<String, String> entry : validEntries) {
            String optName = entry.getKey();
            String optVal = entry.getValue();

            Subquery<Long> voSubquery = query.subquery(Long.class);
            Root<VariantOption> voRoot = voSubquery.from(VariantOption.class);
            Join<VariantOption, ProductOption> poJoin = voRoot.join("productOption");

            List<Predicate> voPredicates = new ArrayList<>();
            voPredicates.add(criteriaBuilder.equal(voRoot.get("productVariant"), pvRoot));

            if (optName != null && !optName.trim().isEmpty()) {
                voPredicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(poJoin.get("name")),
                        optName.trim().toLowerCase()
                ));
            }

            if (optVal != null && !optVal.trim().isEmpty()) {
                voPredicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(voRoot.get("value")),
                        optVal.trim().toLowerCase()
                ));
            }

            voSubquery.select(voRoot.get("id"))
                    .where(criteriaBuilder.and(voPredicates.toArray(new Predicate[0])));

            pvPredicates.add(criteriaBuilder.exists(voSubquery));
        }

        variantSubquery.select(pvRoot.get("id"))
                .where(criteriaBuilder.and(pvPredicates.toArray(new Predicate[0])));

        return criteriaBuilder.exists(variantSubquery);
    }

    public static Specification<Product> hasSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            Join<Product, ProductVariant> variantJoin = getOrCreateVariantJoin(root);
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(variantJoin.get("sku")), searchPattern)
            );
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return criteriaBuilder.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            Join<Product, ProductVariant> variantJoin = getOrCreateVariantJoin(root);
            return criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), minPrice);
        };
    }

    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            Join<Product, ProductVariant> variantJoin = getOrCreateVariantJoin(root);
            return criteriaBuilder.lessThanOrEqualTo(variantJoin.get("price"), maxPrice);
        };
    }

    public static Specification<Product> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            Join<Product, ProductVariant> variantJoin = getOrCreateVariantJoin(root);
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(variantJoin.get("price"), maxPrice));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> hasAttribute(String optionName, String optionValue) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(optionName, optionValue);
        return hasAttributes(attributes);
    }

    public static Specification<Product> hasAttributes(Map<String, String> attributes) {
        return (root, query, criteriaBuilder) -> {
            if (attributes == null || attributes.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            Predicate predicate = buildAttributesPredicate(root, query, criteriaBuilder, attributes);
            return predicate != null ? predicate : criteriaBuilder.conjunction();
        };
    }

    @SuppressWarnings("unchecked")
    private static Join<Product, ProductVariant> getOrCreateVariantJoin(Root<Product> root) {
        for (Join<Product, ?> join : root.getJoins()) {
            if ("variants".equals(join.getAttribute().getName())) {
                return (Join<Product, ProductVariant>) join;
            }
        }
        return root.join("variants", JoinType.LEFT);
    }
}
