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
import com.akbarazy.ecommercebackend.repository.ProductRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category mockCategory;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockCategory = Category.builder()
                .id(1L)
                .name("Apparel")
                .build();

        mockProduct = Product.builder()
                .id(1L)
                .name("Basic T-Shirt")
                .description("Comfortable cotton t-shirt")
                .category(mockCategory)
                .build();
    }

    private ProductRequest buildValidProductRequest() {
        ProductOptionRequest colorOption = ProductOptionRequest.builder()
                .name("Color")
                .values(List.of("Black", "White"))
                .build();

        ProductOptionRequest sizeOption = ProductOptionRequest.builder()
                .name("Size")
                .values(List.of("M", "L"))
                .build();

        VariantOptionRequest var1Color = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest var1Size = VariantOptionRequest.builder()
                .optionName("Size")
                .value("M")
                .build();

        ProductVariantRequest variant1 = ProductVariantRequest.builder()
                .sku("TSHIRT-BLK-M")
                .price(new BigDecimal("150000.00"))
                .stock(10)
                .weight(new BigDecimal("0.25"))
                .variantOptions(List.of(var1Color, var1Size))
                .build();

        return ProductRequest.builder()
                .name("Basic T-Shirt")
                .description("Comfortable cotton t-shirt")
                .categoryId(1L)
                .options(new ArrayList<>(List.of(colorOption, sizeOption)))
                .variants(new ArrayList<>(List.of(variant1)))
                .build();
    }

    // ==================== CREATE PRODUCT SUCCESS ====================

    @Test
    @DisplayName("createProduct should successfully save product with options and variants")
    void createProduct_Success() {
        ProductRequest request = buildValidProductRequest();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            long optionIdSeq = 1L;
            for (ProductOption opt : saved.getOptions()) {
                opt.setId(optionIdSeq++);
            }
            long variantIdSeq = 1L;
            for (ProductVariant var : saved.getVariants()) {
                var.setId(variantIdSeq++);
                long voIdSeq = 1L;
                for (VariantOption vo : var.getValues()) {
                    vo.setId(voIdSeq++);
                }
            }
            return saved;
        });

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("Basic T-Shirt", response.getName());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Apparel", response.getCategoryName());
        assertNotNull(response.getOptions());
        assertEquals(2, response.getOptions().size());
        assertNotNull(response.getVariants());
        assertEquals(1, response.getVariants().size());
        assertEquals("TSHIRT-BLK-M", response.getVariants().get(0).getSku());
        assertEquals(2, response.getVariants().get(0).getOptionValues().size());

        verify(productRepository, times(1)).save(any(Product.class));
    }

    // ==================== CATEGORY NOT FOUND ====================

    @Test
    @DisplayName("createProduct should throw ResourceNotFoundException when category not found")
    void createProduct_CategoryNotFound() {
        ProductRequest request = buildValidProductRequest();
        request.setCategoryId(99L);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    // ==================== REQUIREMENT 2: MINIMAL 1 OPSI & NILAI OPSI ====================

    @Test
    @DisplayName("createProduct should throw BadRequestException when options list is empty")
    void createProduct_EmptyOptions_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        request.setOptions(Collections.emptyList());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertEquals("Product options are required", ex.getMessage());
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when option has empty values list")
    void createProduct_OptionEmptyValues_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        request.getOptions().get(0).setValues(Collections.emptyList());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertEquals("Option values are required", ex.getMessage());
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when option has blank value")
    void createProduct_OptionBlankValue_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        request.getOptions().get(0).setValues(List.of("   "));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("cannot be blank"));
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when duplicate option name exists")
    void createProduct_DuplicateOptionName_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        ProductOptionRequest duplicateOption = ProductOptionRequest.builder()
                .name("color") // duplicate case-insensitive
                .values(List.of("Red"))
                .build();
        request.getOptions().add(duplicateOption);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("Duplicate option name"));
    }

    // ==================== REQUIREMENT 2: MINIMAL 1 VARIAN ====================

    @Test
    @DisplayName("createProduct should throw BadRequestException when variants list is empty")
    void createProduct_EmptyVariants_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        request.setVariants(Collections.emptyList());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertEquals("Product variants are required", ex.getMessage());
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when duplicate SKU exists in request")
    void createProduct_DuplicateSkuInRequest_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        ProductVariantRequest variant2 = ProductVariantRequest.builder()
                .sku("tshirt-blk-m") // duplicate case-insensitive
                .price(new BigDecimal("160000.00"))
                .stock(5)
                .variantOptions(List.of(
                        VariantOptionRequest.builder().optionName("Color").value("White").build(),
                        VariantOptionRequest.builder().optionName("Size").value("L").build()
                ))
                .build();
        request.getVariants().add(variant2);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("Duplicate SKU in request"));
    }

    @Test
    @DisplayName("createProduct should throw ConflictException when SKU already exists in database")
    void createProduct_SkuAlreadyExistsInDb_ThrowsConflictException() {
        ProductRequest request = buildValidProductRequest();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    // ==================== REQUIREMENT 1: KONSISTENSI OPSI DINAMIS ====================

    @Test
    @DisplayName("createProduct should throw BadRequestException when variant is missing a registered option")
    void createProduct_VariantMissingOption_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        // Variant only defines "Color", missing "Size"
        request.getVariants().get(0).setVariantOptions(List.of(
                VariantOptionRequest.builder().optionName("Color").value("Black").build()
        ));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertEquals("Each variant must define values for all registered options", ex.getMessage());
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when variant has unregistered option")
    void createProduct_VariantUnregisteredOption_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        request.getVariants().get(0).setVariantOptions(List.of(
                VariantOptionRequest.builder().optionName("Color").value("Black").build(),
                VariantOptionRequest.builder().optionName("Size").value("M").build(),
                VariantOptionRequest.builder().optionName("Material").value("Cotton").build()
        ));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("is not registered for product"));
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when variant has value not in option allowed values")
    void createProduct_VariantInvalidOptionValue_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        // Size allowed values: "M", "L". Variant specifies "XL".
        request.getVariants().get(0).setVariantOptions(List.of(
                VariantOptionRequest.builder().optionName("Color").value("Black").build(),
                VariantOptionRequest.builder().optionName("Size").value("XL").build()
        ));

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("is not valid for option"));
    }

    @Test
    @DisplayName("createProduct should throw BadRequestException when multiple variants have identical option combinations")
    void createProduct_DuplicateVariantCombination_ThrowsBadRequestException() {
        ProductRequest request = buildValidProductRequest();
        // Add second variant with different SKU but identical option combination (Color: Black, Size: M)
        ProductVariantRequest variant2 = ProductVariantRequest.builder()
                .sku("TSHIRT-BLK-M-2")
                .price(new BigDecimal("150000.00"))
                .stock(5)
                .variantOptions(List.of(
                        VariantOptionRequest.builder().optionName("Color").value("Black").build(),
                        VariantOptionRequest.builder().optionName("Size").value("M").build()
                ))
                .build();
        request.getVariants().add(variant2);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(false);
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M-2")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> productService.createProduct(request));
        assertTrue(ex.getMessage().contains("Duplicate variant option combination found in request"));
    }

    // ==================== OTHER SERVICE METHODS ====================

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals("Basic T-Shirt", response.getName());
    }

    @Test
    void updateProduct_Success() {
        ProductRequest request = ProductRequest.builder()
                .name("Updated T-Shirt")
                .description("Updated description")
                .categoryId(1L)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        ProductResponse response = productService.updateProduct(1L, request);

        assertEquals("Updated T-Shirt", response.getName());
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).delete(mockProduct);
    }
}
