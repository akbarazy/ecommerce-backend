package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.ProductOptionRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductRequest;
import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.request.VariantOptionRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductOptionResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.dto.response.VariantOptionResponse;
import com.akbarazy.ecommercebackend.exception.ConflictException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private ProductRequest createValidRequest() {
        ProductOptionRequest colorOption = ProductOptionRequest.builder()
                .name("Color")
                .values(List.of("Black", "Silver"))
                .build();

        ProductOptionRequest ramOption = ProductOptionRequest.builder()
                .name("RAM")
                .values(List.of("16GB", "32GB"))
                .build();

        VariantOptionRequest varColor = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest varRam = VariantOptionRequest.builder()
                .optionName("RAM")
                .value("16GB")
                .build();

        ProductVariantRequest variant = ProductVariantRequest.builder()
                .sku("LAPTOP-BLK-16")
                .price(new BigDecimal("15000000.00"))
                .stock(10)
                .weight(new BigDecimal("2.20"))
                .imageUrl("https://example.com/laptop-blk.jpg")
                .variantOptions(List.of(varColor, varRam))
                .build();

        return ProductRequest.builder()
                .name("Gaming Laptop")
                .description("High-end gaming laptop")
                .categoryId(1L)
                .options(List.of(colorOption, ramOption))
                .variants(List.of(variant))
                .build();
    }

    private ProductResponse createMockResponse() {
        ProductOptionResponse colorOptionResp = ProductOptionResponse.builder()
                .id(1L)
                .productId(1L)
                .name("Color")
                .values(List.of("Black", "Silver"))
                .build();

        ProductOptionResponse ramOptionResp = ProductOptionResponse.builder()
                .id(2L)
                .productId(1L)
                .name("RAM")
                .values(List.of("16GB", "32GB"))
                .build();

        VariantOptionResponse voColor = VariantOptionResponse.builder()
                .id(1L)
                .optionId(1L)
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionResponse voRam = VariantOptionResponse.builder()
                .id(2L)
                .optionId(2L)
                .optionName("RAM")
                .value("16GB")
                .build();

        ProductVariantResponse variantResp = ProductVariantResponse.builder()
                .id(10L)
                .productId(1L)
                .productName("Gaming Laptop")
                .sku("LAPTOP-BLK-16")
                .price(new BigDecimal("15000000.00"))
                .stock(10)
                .weight(new BigDecimal("2.20"))
                .imageUrl("https://example.com/laptop-blk.jpg")
                .optionValues(List.of(voColor, voRam))
                .createdAt(LocalDateTime.now())
                .build();

        return ProductResponse.builder()
                .id(1L)
                .name("Gaming Laptop")
                .description("High-end gaming laptop")
                .categoryId(1L)
                .categoryName("Electronics")
                .options(List.of(colorOptionResp, ramOptionResp))
                .variants(List.of(variantResp))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== CREATE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 201 when product and initial variants are valid")
    void createProduct_Success() throws Exception {
        ProductRequest request = createValidRequest();
        ProductResponse response = createMockResponse();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.name").value("Gaming Laptop"))
                .andExpect(jsonPath("$.data.options.length()").value(2))
                .andExpect(jsonPath("$.data.variants.length()").value(1))
                .andExpect(jsonPath("$.data.variants[0].sku").value("LAPTOP-BLK-16"))
                .andExpect(jsonPath("$.data.variants[0].price").value(15000000.00))
                .andExpect(jsonPath("$.data.variants[0].stock").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when required fields are missing")
    void createProduct_MissingRequiredFields() throws Exception {
        ProductRequest request = new ProductRequest();

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Product name is required"))
                .andExpect(jsonPath("$.data.categoryId").value("Category ID is required"))
                .andExpect(jsonPath("$.data.options").value("Product options are required"))
                .andExpect(jsonPath("$.data.variants").value("Product variants are required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when initial variants list is empty")
    void createProduct_EmptyVariants() throws Exception {
        ProductRequest request = createValidRequest();
        request.setVariants(Collections.emptyList());

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.variants").value("Product variants are required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when options list is empty")
    void createProduct_EmptyOptions() throws Exception {
        ProductRequest request = createValidRequest();
        request.setOptions(Collections.emptyList());

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.options").value("Product options are required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when an option has empty values list")
    void createProduct_EmptyOptionValues() throws Exception {
        ProductRequest request = createValidRequest();
        request.getOptions().get(0).setValues(Collections.emptyList());

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data['options[0].values']").value("Option values are required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when a variant has empty variantOptions list")
    void createProduct_EmptyVariantOptions() throws Exception {
        ProductRequest request = createValidRequest();
        request.getVariants().get(0).setVariantOptions(Collections.emptyList());

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.variantOptionsValid").value("Each variant must define values for all registered options"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when variant does not match registered options")
    void createProduct_InvalidVariantOptions() throws Exception {
        ProductRequest request = createValidRequest();
        // Only provide Color, missing RAM option
        request.getVariants().get(0).setVariantOptions(List.of(
                VariantOptionRequest.builder().optionName("Color").value("Black").build()
        ));

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.variantOptionsValid").value("Each variant must define values for all registered options"));
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
    @DisplayName("POST /api/admin/products should return 400 when variant price is negative")
    void createProduct_NegativePrice() throws Exception {
        ProductRequest request = createValidRequest();
        request.getVariants().get(0).setPrice(new BigDecimal("-100"));

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data['variants[0].price']").value("Price must be greater than or equal to 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 400 when variant stock is negative")
    void createProduct_NegativeStock() throws Exception {
        ProductRequest request = createValidRequest();
        request.getVariants().get(0).setStock(-5);

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data['variants[0].stock']").value("Stock must be greater than or equal to 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 404 when category not found")
    void createProduct_CategoryNotFound() throws Exception {
        ProductRequest request = createValidRequest();
        request.setCategoryId(99L);

        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category", "id", 99L));

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found with id : '99'"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products should return 409 when variant SKU already exists")
    void createProduct_DuplicateSku() throws Exception {
        ProductRequest request = createValidRequest();

        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new ConflictException("Product variant with SKU 'LAPTOP-BLK-16' already exists"));

        mockMvc.perform(post("/api/admin/products").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product variant with SKU 'LAPTOP-BLK-16' already exists"));
    }

    // ===== UPDATE =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/products/{id} should return 200 when request is valid")
    void updateProduct_Success() throws Exception {
        ProductRequest request = createValidRequest();
        request.setName("Updated Laptop");

        ProductResponse response = createMockResponse();
        response.setName("Updated Laptop");

        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(response);

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
        ProductRequest request = createValidRequest();

        when(productService.updateProduct(eq(99L), any(ProductRequest.class)))
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
