package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CreateCategoryRequest;
import com.akbarazy.ecommercebackend.dto.response.CategoryResponse;
import com.akbarazy.ecommercebackend.entity.Category;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category mockCategory;

    @BeforeEach
    void setUp() {
        mockCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic Items")
                .products(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createCategory_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setDescription("Electronic Items");

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(mockCategory);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Electronics", response.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_NameExists() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");

        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getCategoryById_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertNotNull(response);
        assertEquals("Electronics", response.getName());
    }

    @Test
    void getCategoryById_NotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(99L));
    }

    @Test
    void updateCategory_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("New Electronics");
        request.setDescription("Updated Description");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.existsByName("New Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);

        CategoryResponse response = categoryService.updateCategory(1L, request);

        assertEquals("New Electronics", response.getName());
        assertEquals("Updated Description", response.getDescription());
    }

    @Test
    void deleteCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        assertDoesNotThrow(() -> categoryService.deleteCategory(1L));
        verify(categoryRepository, times(1)).delete(mockCategory);
    }

    @Test
    void deleteCategory_HasProducts() {
        mockCategory.getProducts().add(new Product());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory(1L));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
