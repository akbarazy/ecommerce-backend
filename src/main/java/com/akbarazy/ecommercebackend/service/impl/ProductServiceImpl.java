package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.ProductOptionRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.request.VariantOptionRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import com.akbarazy.ecommercebackend.entity.Category;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.ProductOption;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import com.akbarazy.ecommercebackend.entity.VariantOption;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ConflictException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CategoryRepository;
import com.akbarazy.ecommercebackend.repository.ProductOptionRepository;
import com.akbarazy.ecommercebackend.repository.ProductRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import com.akbarazy.ecommercebackend.service.ProductService;
import com.akbarazy.ecommercebackend.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // 1 & 2. Validations: Requirement 1 (konsistensi opsi dinamis) & Requirement 2 (minimal 1 varian & 1 nilai opsi)
        validateCreateProductRequest(request);

        // 3. Simpan Product beserta ProductOption dan initial ProductVariant secara transaksional
        Product product = Product.builder()
            .name(request.getName().trim())
            .description(request.getDescription())
            .category(category)
            .build();

        Map<String, ProductOption> optionEntityMap = new HashMap<>();
        Map<String, ProductOptionRequest> optionRequestMap = new HashMap<>();

        for (ProductOptionRequest optionReq : request.getOptions()) {
            ProductOption productOption = ProductOption.builder()
                .name(optionReq.getName().trim())
                .build();
            product.addOption(productOption);

            String normalizedName = productOption.getName().toLowerCase();
            optionEntityMap.put(normalizedName, productOption);
            optionRequestMap.put(normalizedName, optionReq);
        }

        for (ProductVariantRequest variantReq : request.getVariants()) {
            ProductVariant variant = ProductVariant.builder()
                .sku(variantReq.getSku().trim())
                .price(variantReq.getPrice())
                .stock(variantReq.getStock() != null ? variantReq.getStock() : 0)
                .weight(variantReq.getWeight())
                .imageUrl(variantReq.getImageUrl())
                .build();

            product.addVariant(variant);

            for (VariantOptionRequest voReq : variantReq.getVariantOptions()) {
                String normalizedOptionName = voReq.getOptionName().trim().toLowerCase();
                ProductOption productOption = optionEntityMap.get(normalizedOptionName);
                ProductOptionRequest registeredOptionReq = optionRequestMap.get(normalizedOptionName);

                String matchedValue = registeredOptionReq.getValues().stream()
                    .filter(v -> v.trim().equalsIgnoreCase(voReq.getValue().trim()))
                    .findFirst()
                    .orElse(voReq.getValue().trim());

                VariantOption variantOption = VariantOption.builder()
                    .productVariant(variant)
                    .productOption(productOption)
                    .value(matchedValue)
                    .build();

                variant.addValue(variantOption);
                productOption.addValue(variantOption);
            }
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
    }

    private void validateCreateProductRequest(ProductRequest request) {
        // Requirement 2: validasi minimal 1 nilai opsi (dan minimal 1 opsi)
        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new BadRequestException("Product options are required");
        }

        Set<String> seenOptionNames = new HashSet<>();
        Map<String, ProductOptionRequest> optionMap = new HashMap<>();

        for (ProductOptionRequest optionReq : request.getOptions()) {
            if (optionReq == null || optionReq.getName() == null || optionReq.getName().trim().isEmpty()) {
                throw new BadRequestException("Option name is required");
            }
            String normalizedOptionName = optionReq.getName().trim().toLowerCase();
            if (!seenOptionNames.add(normalizedOptionName)) {
                throw new BadRequestException("Duplicate option name: " + optionReq.getName());
            }

            if (optionReq.getValues() == null || optionReq.getValues().isEmpty()) {
                throw new BadRequestException("Option values are required");
            }

            Set<String> seenValues = new HashSet<>();
            for (String val : optionReq.getValues()) {
                if (val == null || val.trim().isEmpty()) {
                    throw new BadRequestException("Option value cannot be blank for option '" + optionReq.getName() + "'");
                }
                if (!seenValues.add(val.trim().toLowerCase())) {
                    throw new BadRequestException("Duplicate option value '" + val + "' for option '" + optionReq.getName() + "'");
                }
            }

            optionMap.put(normalizedOptionName, optionReq);
        }

        // Requirement 2: validasi minimal 1 varian
        if (request.getVariants() == null || request.getVariants().isEmpty()) {
            throw new BadRequestException("Product variants are required");
        }

        Set<String> seenSkus = new HashSet<>();
        Set<String> seenCombinations = new HashSet<>();

        // Requirement 1: validasi konsistensi opsi dinamis
        for (ProductVariantRequest variantReq : request.getVariants()) {
            if (variantReq == null) {
                throw new BadRequestException("Variant cannot be null");
            }

            String sku = variantReq.getSku();
            if (sku == null || sku.trim().isEmpty()) {
                throw new BadRequestException("SKU is required");
            }
            String normalizedSku = sku.trim().toLowerCase();
            if (!seenSkus.add(normalizedSku)) {
                throw new BadRequestException("Duplicate SKU in request: " + sku);
            }
            if (productVariantRepository.existsBySku(sku.trim())) {
                throw new ConflictException("Product variant with SKU '" + sku + "' already exists");
            }

            if (variantReq.getPrice() == null) {
                throw new BadRequestException("Price is required");
            }
            if (variantReq.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Price must be greater than or equal to 0");
            }

            if (variantReq.getStock() != null && variantReq.getStock() < 0) {
                throw new BadRequestException("Stock must be greater than or equal to 0");
            }

            if (variantReq.getWeight() != null && variantReq.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Weight must be greater than or equal to 0");
            }

            List<VariantOptionRequest> variantOptions = variantReq.getVariantOptions();
            if (variantOptions == null || variantOptions.isEmpty()) {
                throw new BadRequestException("Variant with SKU '" + sku + "' must define option values");
            }

            Set<String> definedOptionNames = new HashSet<>();
            for (VariantOptionRequest voReq : variantOptions) {
                if (voReq == null) {
                    throw new BadRequestException("Variant option cannot be null for variant with SKU " + sku);
                }
                String optionName = voReq.getOptionName();
                if (optionName == null || optionName.trim().isEmpty()) {
                    throw new BadRequestException("Option name is required for variant with SKU " + sku);
                }
                String normalizedOptionName = optionName.trim().toLowerCase();
                ProductOptionRequest registeredOption = optionMap.get(normalizedOptionName);
                if (registeredOption == null) {
                    throw new BadRequestException("Option '" + optionName + "' is not registered for product");
                }

                if (!definedOptionNames.add(normalizedOptionName)) {
                    throw new BadRequestException("Duplicate option '" + optionName + "' in variant with SKU " + sku);
                }

                String value = voReq.getValue();
                if (value == null || value.trim().isEmpty()) {
                    throw new BadRequestException("Option value is required for option '" + optionName + "' in variant with SKU " + sku);
                }

                boolean valueMatches = registeredOption.getValues().stream()
                    .anyMatch(v -> v.trim().equalsIgnoreCase(value.trim()));
                if (!valueMatches) {
                    throw new BadRequestException("Value '" + value + "' is not valid for option '" + registeredOption.getName() + "'");
                }
            }

            if (!definedOptionNames.equals(optionMap.keySet())) {
                throw new BadRequestException("Each variant must define values for all registered options");
            }

            // Check duplicate variant combination
            String combinationKey = variantOptions.stream()
                .sorted(Comparator.comparing(vo -> vo.getOptionName().trim().toLowerCase()))
                .map(vo -> vo.getOptionName().trim().toLowerCase() + "=" + vo.getValue().trim().toLowerCase())
                .collect(Collectors.joining("|"));

            if (!seenCombinations.add(combinationKey)) {
                throw new BadRequestException("Duplicate variant option combination found in request");
            }
        }
    }

    @Override
    public ProductResponse getProductById(Long id) {
        return ProductResponse.from(getProductEntityById(id));
    }

    @Override
    public Page<ProductResponse> getAllProducts(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = ProductSpecification.filterProducts(search, categoryId, minPrice, maxPrice);
        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(ProductResponse::from);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductEntityById(id);
        
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductEntityById(id);
        productRepository.delete(product);
    }

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
}
