package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.CreateCategoryRequest;
import com.akbarazy.ecommercebackend.dto.response.CategoryResponse;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private CreateCategoryRequest createValidRequest() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setDescription("Electronic items");
        return request;
    }

    private CategoryResponse createMockResponse() {
        return CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic items")
                .productCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== CREATE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/categories should return 201 when request is valid")
    void createCategory_Success() throws Exception {
        CreateCategoryRequest request = createValidRequest();
        CategoryResponse response = createMockResponse();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.data.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/categories should return 400 when name is blank")
    void createCategory_BlankName() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();

        mockMvc.perform(post("/api/admin/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Category name is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/categories should return 400 when request body is missing")
    void createCategory_MissingBody() throws Exception {
        mockMvc.perform(post("/api/admin/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is missing or unreadable"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/categories should return 400 when name already exists")
    void createCategory_DuplicateName() throws Exception {
        CreateCategoryRequest request = createValidRequest();

        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenThrow(new BadRequestException("Category name already exists"));

        mockMvc.perform(post("/api/admin/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category name already exists"));
    }

    // ===== UPDATE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/categories/{id} should return 200 when request is valid")
    void updateCategory_Success() throws Exception {
        CreateCategoryRequest request = createValidRequest();
        request.setName("Updated Electronics");

        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .name("Updated Electronics")
                .description("Electronic items")
                .productCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryService.updateCategory(eq(1L), any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/categories/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Electronics"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/categories/{id} should return 404 when category not found")
    void updateCategory_NotFound() throws Exception {
        CreateCategoryRequest request = createValidRequest();

        when(categoryService.updateCategory(eq(99L), any(CreateCategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category", "id", 99L));

        mockMvc.perform(put("/api/admin/categories/99").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== DELETE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/categories/{id} should return 200 when category is deleted")
    void deleteCategory_Success() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));

        verify(categoryService, times(1)).deleteCategory(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/categories/{id} should return 400 when category has products")
    void deleteCategory_HasProducts() throws Exception {
        doThrow(new BadRequestException("Cannot delete category that still has products"))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete category that still has products"));
    }
}
