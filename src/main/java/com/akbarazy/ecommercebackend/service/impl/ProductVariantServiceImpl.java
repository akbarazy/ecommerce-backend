package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.request.VariantOptionRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.ProductOption;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import com.akbarazy.ecommercebackend.entity.VariantOption;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ConflictException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CartItemRepository;
import com.akbarazy.ecommercebackend.repository.OrderItemRepository;
import com.akbarazy.ecommercebackend.repository.ProductRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import com.akbarazy.ecommercebackend.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public ProductVariantResponse addVariantToProduct(Long productId, ProductVariantRequest request) {
        if (productId == null) {
            throw new BadRequestException("Product ID is required");
        }
        if (request == null) {
            throw new BadRequestException("Variant request cannot be null");
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        validateBasicVariantFields(request);

        String trimmedSku = request.getSku().trim();
        if (productVariantRepository.existsBySku(trimmedSku)) {
            throw new ConflictException("Product variant with SKU '" + trimmedSku + "' already exists");
        }

        List<VariantOption> validatedVariantOptions = validateAndBuildVariantOptions(
            product,
            request.getVariantOptions(),
            null
        );

        ProductVariant variant = ProductVariant.builder()
            .sku(trimmedSku)
            .price(request.getPrice())
            .stock(request.getStock() != null ? request.getStock() : 0)
            .weight(request.getWeight())
            .imageUrl(request.getImageUrl())
            .build();

        product.addVariant(variant);

        for (VariantOption vo : validatedVariantOptions) {
            variant.addValue(vo);
            vo.getProductOption().addValue(vo);
        }

        ProductVariant savedVariant = productVariantRepository.save(variant);
        return ProductVariantResponse.from(savedVariant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        return variants.stream()
            .map(ProductVariantResponse::from)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));
        return ProductVariantResponse.from(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long id, ProductVariantRequest request) {
        if (request == null) {
            throw new BadRequestException("Variant request cannot be null");
        }

        ProductVariant variant = productVariantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));

        validateBasicVariantFields(request);

        String trimmedSku = request.getSku().trim();
        if (productVariantRepository.existsBySkuAndIdNot(trimmedSku, id)) {
            throw new ConflictException("Product variant with SKU '" + trimmedSku + "' already exists");
        }

        Product product = variant.getProduct();
        if (product == null) {
            throw new BadRequestException("Variant is not associated with any product");
        }

        if (request.getVariantOptions() != null && !request.getVariantOptions().isEmpty()) {
            List<VariantOption> validatedVariantOptions = validateAndBuildVariantOptions(
                product,
                request.getVariantOptions(),
                id
            );

            // Update existing or replace variant options
            Map<Long, VariantOption> existingByOptionId = new HashMap<>();
            if (variant.getValues() != null) {
                for (VariantOption existingVo : new ArrayList<>(variant.getValues())) {
                    if (existingVo.getProductOption() != null && existingVo.getProductOption().getId() != null) {
                        existingByOptionId.put(existingVo.getProductOption().getId(), existingVo);
                    }
                }
            }

            Set<Long> updatedOptionIds = new HashSet<>();
            for (VariantOption newVo : validatedVariantOptions) {
                ProductOption opt = newVo.getProductOption();
                Long optId = opt.getId();
                if (optId != null && existingByOptionId.containsKey(optId)) {
                    VariantOption existingVo = existingByOptionId.get(optId);
                    existingVo.setValue(newVo.getValue());
                    updatedOptionIds.add(optId);
                } else {
                    variant.addValue(newVo);
                    opt.addValue(newVo);
                }
            }

            // Remove any stale variant options not in updated set
            if (variant.getValues() != null) {
                for (VariantOption existingVo : new ArrayList<>(variant.getValues())) {
                    Long optId = existingVo.getProductOption() != null ? existingVo.getProductOption().getId() : null;
                    if (optId != null && !updatedOptionIds.contains(optId) && existingByOptionId.containsKey(optId)) {
                        variant.removeValue(existingVo);
                        existingVo.getProductOption().removeValue(existingVo);
                    }
                }
            }
        } else if (product.getOptions() != null && !product.getOptions().isEmpty()) {
            throw new BadRequestException("Variant with SKU '" + trimmedSku + "' must define option values");
        }

        variant.setSku(trimmedSku);
        variant.setPrice(request.getPrice());
        variant.setStock(request.getStock() != null ? request.getStock() : 0);
        variant.setWeight(request.getWeight());
        variant.setImageUrl(request.getImageUrl());

        ProductVariant savedVariant = productVariantRepository.save(variant);
        return ProductVariantResponse.from(savedVariant);
    }

    @Override
    @Transactional
    public void deleteVariant(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));

        // 1. Cek keterkaitan dengan order
        if (orderItemRepository.existsByVariantId(id)) {
            throw new ConflictException("Cannot delete variant because it is associated with existing orders");
        }

        // 2. Validasi agar tidak menghapus varian terakhir jika produk aktif
        Product product = variant.getProduct();
        if (product != null) {
            List<ProductVariant> productVariants = productVariantRepository.findByProductId(product.getId());
            int variantCount = (productVariants != null && !productVariants.isEmpty())
                ? productVariants.size()
                : (product.getVariants() != null ? product.getVariants().size() : 0);

            if (variantCount <= 1) {
                throw new BadRequestException("Cannot delete the last variant of an active product");
            }

            product.removeVariant(variant);
        }

        if (variant.getValues() != null) {
            for (VariantOption vo : new ArrayList<>(variant.getValues())) {
                if (vo.getProductOption() != null) {
                    vo.getProductOption().removeValue(vo);
                }
                variant.removeValue(vo);
            }
        }

        if (cartItemRepository != null) {
            cartItemRepository.deleteByVariantId(id);
        }

        productVariantRepository.delete(variant);
    }

    private void validateBasicVariantFields(ProductVariantRequest request) {
        if (request.getSku() == null || request.getSku().trim().isEmpty()) {
            throw new BadRequestException("SKU is required");
        }
        if (request.getPrice() == null) {
            throw new BadRequestException("Price is required");
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Price must be greater than or equal to 0");
        }
        if (request.getStock() != null && request.getStock() < 0) {
            throw new BadRequestException("Stock must be greater than or equal to 0");
        }
        if (request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Weight must be greater than or equal to 0");
        }
    }

    private List<VariantOption> validateAndBuildVariantOptions(
            Product product,
            List<VariantOptionRequest> variantOptionRequests,
            Long excludeVariantId) {

        List<ProductOption> productOptions = product.getOptions() != null ? product.getOptions() : Collections.emptyList();
        if (productOptions.isEmpty()) {
            throw new BadRequestException("Product has no registered options");
        }

        if (variantOptionRequests == null || variantOptionRequests.isEmpty()) {
            throw new BadRequestException("Variant must define option values");
        }

        Map<String, ProductOption> optionByName = new HashMap<>();
        Map<Long, ProductOption> optionById = new HashMap<>();

        for (ProductOption opt : productOptions) {
            if (opt.getName() != null) {
                optionByName.put(opt.getName().trim().toLowerCase(), opt);
            }
            if (opt.getId() != null) {
                optionById.put(opt.getId(), opt);
            }
        }

        Set<String> definedOptionNames = new HashSet<>();
        List<VariantOption> result = new ArrayList<>();

        for (VariantOptionRequest voReq : variantOptionRequests) {
            if (voReq == null) {
                throw new BadRequestException("Variant option cannot be null");
            }

            ProductOption matchedOption = null;
            if (voReq.getOptionId() != null) {
                matchedOption = optionById.get(voReq.getOptionId());
                if (matchedOption == null) {
                    throw new BadRequestException("Option with ID '" + voReq.getOptionId() + "' is not registered for product");
                }
                if (voReq.getOptionName() != null && !voReq.getOptionName().trim().isEmpty()) {
                    if (!matchedOption.getName().trim().equalsIgnoreCase(voReq.getOptionName().trim())) {
                        throw new BadRequestException("Option ID '" + voReq.getOptionId() + "' does not match option name '" + voReq.getOptionName() + "'");
                    }
                }
            } else if (voReq.getOptionName() != null && !voReq.getOptionName().trim().isEmpty()) {
                matchedOption = optionByName.get(voReq.getOptionName().trim().toLowerCase());
                if (matchedOption == null) {
                    throw new BadRequestException("Option '" + voReq.getOptionName() + "' is not registered for product");
                }
            } else {
                throw new BadRequestException("Option name or option ID is required for variant option");
            }

            String normalizedOptionName = matchedOption.getName().trim().toLowerCase();
            if (!definedOptionNames.add(normalizedOptionName)) {
                throw new BadRequestException("Duplicate option '" + matchedOption.getName() + "' in variant");
            }

            if (voReq.getValue() == null || voReq.getValue().trim().isEmpty()) {
                throw new BadRequestException("Option value is required for option '" + matchedOption.getName() + "'");
            }

            String trimmedValue = voReq.getValue().trim();
            // Match casing with existing option value if already present
            String resolvedValue = trimmedValue;
            if (matchedOption.getValues() != null) {
                resolvedValue = matchedOption.getValues().stream()
                    .map(VariantOption::getValue)
                    .filter(Objects::nonNull)
                    .filter(v -> v.trim().equalsIgnoreCase(trimmedValue))
                    .findFirst()
                    .orElse(trimmedValue);
            }

            VariantOption vo = VariantOption.builder()
                .productOption(matchedOption)
                .value(resolvedValue)
                .build();
            result.add(vo);
        }

        if (!definedOptionNames.equals(optionByName.keySet())) {
            throw new BadRequestException("Each variant must define values for all registered options");
        }

        // Check duplicate combination against existing variants of the product
        String newCombinationKey = buildCombinationKeyFromVariantOptions(result);

        List<ProductVariant> existingVariants = product.getVariants();
        if (existingVariants == null || existingVariants.isEmpty()) {
            existingVariants = productVariantRepository.findByProductId(product.getId());
        }

        if (existingVariants != null) {
            for (ProductVariant existing : existingVariants) {
                if (excludeVariantId != null && excludeVariantId.equals(existing.getId())) {
                    continue;
                }
                if (existing.getValues() != null && !existing.getValues().isEmpty()) {
                    String existingKey = buildCombinationKeyFromVariantOptions(existing.getValues());
                    if (newCombinationKey.equals(existingKey)) {
                        throw new ConflictException("Variant with the same option combination already exists for this product");
                    }
                }
            }
        }

        return result;
    }

    private String buildCombinationKeyFromVariantOptions(List<VariantOption> options) {
        return options.stream()
            .filter(vo -> vo.getProductOption() != null && vo.getProductOption().getName() != null && vo.getValue() != null)
            .sorted(Comparator.comparing(vo -> vo.getProductOption().getName().trim().toLowerCase()))
            .map(vo -> vo.getProductOption().getName().trim().toLowerCase() + "=" + vo.getValue().trim().toLowerCase())
            .collect(Collectors.joining("|"));
    }
}

