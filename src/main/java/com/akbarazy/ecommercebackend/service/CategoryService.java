package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.CreateCategoryRequest;
import com.akbarazy.ecommercebackend.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategory(Long id, CreateCategoryRequest request);
    void deleteCategory(Long id);
}
