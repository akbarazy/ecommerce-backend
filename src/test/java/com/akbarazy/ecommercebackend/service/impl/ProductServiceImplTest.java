package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CreateProductRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import com.akbarazy.ecommercebackend.entity.Category;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CategoryRepository;
import com.akbarazy.ecommercebackend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @InjectMocks
    private ProductServiceImpl productService;

    private Category mockCategory;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        mockProduct = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Gaming Laptop")
                .price(new BigDecimal("15000000.00"))
                .stock(10)
                .category(mockCategory)
                .build();
    }

    @Test
    void createProduct_Success() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Laptop");
        request.setPrice(new BigDecimal("15000000.00"));
        request.setStock(10);
        request.setCategoryId(1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("Laptop", response.getName());
        assertEquals(1L, response.getCategoryId());
    }

    @Test
    void createProduct_CategoryNotFound() {
        CreateProductRequest request = new CreateProductRequest();
        request.setCategoryId(99L);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(request));
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals("Laptop", response.getName());
    }

    @Test
    void updateProduct_Success() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated Laptop");
        request.setPrice(new BigDecimal("14000000.00"));
        request.setStock(5);
        request.setCategoryId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        ProductResponse response = productService.updateProduct(1L, request);

        assertEquals("Updated Laptop", response.getName());
        assertEquals(5, response.getStock());
    }

    @Test
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, times(1)).delete(mockProduct);
    }
}
