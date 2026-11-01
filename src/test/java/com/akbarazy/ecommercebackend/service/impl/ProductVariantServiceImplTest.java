package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.request.VariantOptionRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.entity.Category;
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
class ProductVariantServiceImplTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private ProductVariantServiceImpl productVariantService;

    private Product mockProduct;
    private ProductOption colorOption;
    private ProductOption sizeOption;
    private ProductVariant existingVariant;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
            .id(1L)
            .name("Apparel")
            .build();

        mockProduct = Product.builder()
            .id(1L)
            .name("Basic T-Shirt")
            .description("Cotton T-Shirt")
            .category(category)
            .options(new ArrayList<>())
            .variants(new ArrayList<>())
            .build();

        colorOption = ProductOption.builder()
            .id(10L)
            .name("Color")
            .values(new ArrayList<>())
            .build();

        sizeOption = ProductOption.builder()
            .id(11L)
            .name("Size")
            .values(new ArrayList<>())
            .build();

        mockProduct.addOption(colorOption);
        mockProduct.addOption(sizeOption);

        existingVariant = ProductVariant.builder()
            .id(100L)
            .sku("TSHIRT-BLK-M")
            .price(new BigDecimal("150000.00"))
            .stock(10)
            .weight(new BigDecimal("0.25"))
            .values(new ArrayList<>())
            .build();

        VariantOption voColor = VariantOption.builder()
            .id(1000L)
            .productOption(colorOption)
            .value("Black")
            .build();

        VariantOption voSize = VariantOption.builder()
            .id(1001L)
            .productOption(sizeOption)
            .value("M")
            .build();

        existingVariant.addValue(voColor);
        existingVariant.addValue(voSize);
        colorOption.addValue(voColor);
        sizeOption.addValue(voSize);
        mockProduct.addVariant(existingVariant);
    }

    private ProductVariantRequest buildNewVariantRequest() {
        return ProductVariantRequest.builder()
            .sku("TSHIRT-WHT-L")
            .price(new BigDecimal("160000.00"))
            .stock(15)
            .weight(new BigDecimal("0.30"))
            .imageUrl("https://example.com/tshirt-wht.jpg")
            .variantOptions(List.of(
                VariantOptionRequest.builder().optionName("Color").value("White").build(),
                VariantOptionRequest.builder().optionName("Size").value("L").build()
            ))
            .build();
    }

    // ==================== 1. addVariantToProduct ====================

    @Test
    @DisplayName("addVariantToProduct should add new variant when SKU is unique and options match")
    void addVariantToProduct_Success() {
        ProductVariantRequest request = buildNewVariantRequest();

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("TSHIRT-WHT-L")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        ProductVariantResponse response = productVariantService.addVariantToProduct(1L, request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("TSHIRT-WHT-L", response.getSku());
        assertEquals(new BigDecimal("160000.00"), response.getPrice());
        assertEquals(15, response.getStock());
        assertEquals(2, response.getOptionValues().size());

        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("addVariantToProduct should throw ResourceNotFoundException when product does not exist")
    void addVariantToProduct_ProductNotFound() {
        ProductVariantRequest request = buildNewVariantRequest();
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> productVariantService.addVariantToProduct(99L, request));
        verify(productVariantRepository, never()).save(any());
    }

    @Test
    @DisplayName("addVariantToProduct should throw ConflictException when SKU already exists")
    void addVariantToProduct_DuplicateSku_ThrowsConflictException() {
        ProductVariantRequest request = buildNewVariantRequest();
        request.setSku("TSHIRT-BLK-M");

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("TSHIRT-BLK-M")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
            () -> productVariantService.addVariantToProduct(1L, request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("addVariantToProduct should throw BadRequestException when option does not match product options")
    void addVariantToProduct_UnregisteredOption_ThrowsBadRequestException() {
        ProductVariantRequest request = buildNewVariantRequest();
        request.setVariantOptions(List.of(
            VariantOptionRequest.builder().optionName("Color").value("White").build(),
            VariantOptionRequest.builder().optionName("Material").value("Cotton").build()
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("TSHIRT-WHT-L")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> productVariantService.addVariantToProduct(1L, request));
        assertTrue(ex.getMessage().contains("is not registered for product"));
    }

    @Test
    @DisplayName("addVariantToProduct should throw BadRequestException when missing a registered option")
    void addVariantToProduct_MissingOption_ThrowsBadRequestException() {
        ProductVariantRequest request = buildNewVariantRequest();
        request.setVariantOptions(List.of(
            VariantOptionRequest.builder().optionName("Color").value("White").build()
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("TSHIRT-WHT-L")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> productVariantService.addVariantToProduct(1L, request));
        assertEquals("Each variant must define values for all registered options", ex.getMessage());
    }

    @Test
    @DisplayName("addVariantToProduct should throw ConflictException when option combination already exists")
    void addVariantToProduct_DuplicateOptionCombination_ThrowsConflictException() {
        ProductVariantRequest request = buildNewVariantRequest();
        // Same combination as existingVariant (Black, M)
        request.setVariantOptions(List.of(
            VariantOptionRequest.builder().optionName("Color").value("Black").build(),
            VariantOptionRequest.builder().optionName("Size").value("M").build()
        ));

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("TSHIRT-WHT-L")).thenReturn(false);

        ConflictException ex = assertThrows(ConflictException.class,
            () -> productVariantService.addVariantToProduct(1L, request));
        assertTrue(ex.getMessage().contains("same option combination already exists"));
    }

    // ==================== 2. getVariantsByProductId & getVariantById ====================

    @Test
    @DisplayName("getVariantsByProductId should return all variants for product")
    void getVariantsByProductId_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(existingVariant));

        List<ProductVariantResponse> responses = productVariantService.getVariantsByProductId(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("TSHIRT-BLK-M", responses.get(0).getSku());
    }

    @Test
    @DisplayName("getVariantsByProductId should throw ResourceNotFoundException when product not found")
    void getVariantsByProductId_ProductNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> productVariantService.getVariantsByProductId(99L));
    }

    @Test
    @DisplayName("getVariantById should return variant details")
    void getVariantById_Success() {
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));

        ProductVariantResponse response = productVariantService.getVariantById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("TSHIRT-BLK-M", response.getSku());
    }

    @Test
    @DisplayName("getVariantById should throw ResourceNotFoundException when variant not found")
    void getVariantById_NotFound() {
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> productVariantService.getVariantById(999L));
    }

    // ==================== 3. updateVariant ====================

    @Test
    @DisplayName("updateVariant should update price, stock, SKU, and option combination")
    void updateVariant_Success() {
        ProductVariantRequest updateRequest = ProductVariantRequest.builder()
            .sku("TSHIRT-NAVY-XL")
            .price(new BigDecimal("175000.00"))
            .stock(25)
            .weight(new BigDecimal("0.28"))
            .imageUrl("https://example.com/navy.jpg")
            .variantOptions(List.of(
                VariantOptionRequest.builder().optionId(10L).optionName("Color").value("Navy").build(),
                VariantOptionRequest.builder().optionId(11L).optionName("Size").value("XL").build()
            ))
            .build();

        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsBySkuAndIdNot("TSHIRT-NAVY-XL", 100L)).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariantResponse response = productVariantService.updateVariant(100L, updateRequest);

        assertNotNull(response);
        assertEquals("TSHIRT-NAVY-XL", response.getSku());
        assertEquals(new BigDecimal("175000.00"), response.getPrice());
        assertEquals(25, response.getStock());
        assertEquals(2, response.getOptionValues().size());
        assertTrue(response.getOptionValues().stream().anyMatch(vo -> vo.getValue().equals("Navy")));
        assertTrue(response.getOptionValues().stream().anyMatch(vo -> vo.getValue().equals("XL")));
    }

    @Test
    @DisplayName("updateVariant should throw ConflictException when updated SKU belongs to another variant")
    void updateVariant_DuplicateSku_ThrowsConflictException() {
        ProductVariantRequest updateRequest = buildNewVariantRequest();

        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsBySkuAndIdNot("TSHIRT-WHT-L", 100L)).thenReturn(true);

        assertThrows(ConflictException.class,
            () -> productVariantService.updateVariant(100L, updateRequest));
    }

    @Test
    @DisplayName("updateVariant should throw ConflictException when updated option combination conflicts with another variant")
    void updateVariant_DuplicateOptionCombination_ThrowsConflictException() {
        // Add second variant to product (White, L)
        ProductVariant secondVariant = ProductVariant.builder()
            .id(101L)
            .sku("TSHIRT-WHT-L")
            .price(new BigDecimal("160000.00"))
            .stock(5)
            .values(new ArrayList<>())
            .build();
        VariantOption vo2Color = VariantOption.builder().productOption(colorOption).value("White").build();
        VariantOption vo2Size = VariantOption.builder().productOption(sizeOption).value("L").build();
        secondVariant.addValue(vo2Color);
        secondVariant.addValue(vo2Size);
        mockProduct.addVariant(secondVariant);

        // Try updating variant 100L to also have (White, L)
        ProductVariantRequest updateRequest = buildNewVariantRequest();
        updateRequest.setSku("TSHIRT-BLK-M");

        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));
        when(productVariantRepository.existsBySkuAndIdNot("TSHIRT-BLK-M", 100L)).thenReturn(false);

        assertThrows(ConflictException.class,
            () -> productVariantService.updateVariant(100L, updateRequest));
    }

    // ==================== 4. deleteVariant ====================

    @Test
    @DisplayName("deleteVariant should delete variant when product has multiple variants and variant has no orders")
    void deleteVariant_Success() {
        ProductVariant secondVariant = ProductVariant.builder()
            .id(101L)
            .sku("TSHIRT-WHT-L")
            .price(new BigDecimal("160000.00"))
            .stock(5)
            .build();
        mockProduct.addVariant(secondVariant);

        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));
        when(orderItemRepository.existsByVariantId(100L)).thenReturn(false);
        when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(existingVariant, secondVariant));

        assertDoesNotThrow(() -> productVariantService.deleteVariant(100L));

        verify(cartItemRepository, times(1)).deleteByVariantId(100L);
        verify(productVariantRepository, times(1)).delete(existingVariant);
    }

    @Test
    @DisplayName("deleteVariant should throw ConflictException when variant is associated with orders")
    void deleteVariant_AssociatedWithOrder_ThrowsConflictException() {
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));
        when(orderItemRepository.existsByVariantId(100L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
            () -> productVariantService.deleteVariant(100L));
        assertTrue(ex.getMessage().contains("associated with existing orders"));
        verify(productVariantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteVariant should throw BadRequestException when trying to delete the last variant of an active product")
    void deleteVariant_LastVariant_ThrowsBadRequestException() {
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(existingVariant));
        when(orderItemRepository.existsByVariantId(100L)).thenReturn(false);
        when(productVariantRepository.findByProductId(1L)).thenReturn(List.of(existingVariant));

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> productVariantService.deleteVariant(100L));
        assertTrue(ex.getMessage().contains("Cannot delete the last variant"));
        verify(productVariantRepository, never()).delete(any());
    }
}

