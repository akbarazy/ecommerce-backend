package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.CreateProductRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private CreateProductRequest createValidRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Gaming Laptop");
        request.setDescription("High-end gaming laptop");
        request.setPrice(new BigDecimal("15000000.00"));
        request.setStock(10);
        request.setImageUrl("https://example.com/laptop.jpg");
        request.setCategoryId(1L);
        return request;
    }

    private ProductResponse createMockResponse() {
        return ProductResponse.builder()
                .id(1L)
                .name("Gaming Laptop")
                .description("High-end gaming laptop")
                .price(new BigDecimal("15000000.00"))
                .stock(10)
                .imageUrl("https://example.com/laptop.jpg")
                .categoryId(1L)
                .categoryName("Electronics")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== CREATE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 201 when request is valid")
    void createProduct_Success() throws Exception {
        CreateProductRequest request = createValidRequest();
        ProductResponse response = createMockResponse();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.data.price").value(15000000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when required fields are missing")
    void createProduct_MissingRequiredFields() throws Exception {
        CreateProductRequest request = new CreateProductRequest();

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Product name is required"))
                .andExpect(jsonPath("$.data.price").value("Price is required"))
                .andExpect(jsonPath("$.data.stock").value("Stock is required"))
                .andExpect(jsonPath("$.data.categoryId").value("Category ID is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when request body is missing")
    void createProduct_MissingBody() throws Exception {
        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is missing or unreadable"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when price is negative")
    void createProduct_NegativePrice() throws Exception {
        CreateProductRequest request = createValidRequest();
        request.setPrice(new BigDecimal("-100"));

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.price").value("Price must be greater than or equal to 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when stock is negative")
    void createProduct_NegativeStock() throws Exception {
        CreateProductRequest request = createValidRequest();
        request.setStock(-5);

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.stock").value("Stock must be greater than or equal to 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 404 when category not found")
    void createProduct_CategoryNotFound() throws Exception {
        CreateProductRequest request = createValidRequest();
        request.setCategoryId(99L);

        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category", "id", 99L));

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found with id : '99'"));
    }

    // ===== UPDATE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/products/{id} should return 200 when request is valid")
    void updateProduct_Success() throws Exception {
        CreateProductRequest request = createValidRequest();
        request.setName("Updated Laptop");

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Updated Laptop")
                .price(new BigDecimal("15000000.00"))
                .categoryId(1L)
                .categoryName("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        when(productService.updateProduct(eq(1L), any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/products/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Laptop"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/products/{id} should return 404 when product not found")
    void updateProduct_NotFound() throws Exception {
        CreateProductRequest request = createValidRequest();

        when(productService.updateProduct(eq(99L), any(CreateProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(put("/api/admin/products/99").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== DELETE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/products/{id} should return 200 when product is deleted")
    void deleteProduct_Success() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/admin/products/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/products/{id} should return 404 when product not found")
    void deleteProduct_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Product", "id", 99L))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/admin/products/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
