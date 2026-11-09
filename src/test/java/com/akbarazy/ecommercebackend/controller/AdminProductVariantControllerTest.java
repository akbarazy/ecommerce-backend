package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.config.SecurityConfig;
import com.akbarazy.ecommercebackend.dto.request.ProductVariantRequest;
import com.akbarazy.ecommercebackend.dto.request.VariantOptionRequest;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.dto.response.VariantOptionResponse;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ConflictException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.service.ProductVariantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductVariantController.class)
@Import(SecurityConfig.class)
class AdminProductVariantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductVariantService productVariantService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private ProductVariantRequest createValidRequest() {
        VariantOptionRequest colorOption = VariantOptionRequest.builder()
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionRequest sizeOption = VariantOptionRequest.builder()
                .optionName("Size")
                .value("L")
                .build();

        return ProductVariantRequest.builder()
                .sku("TSHIRT-BLK-L")
                .price(new BigDecimal("150000.00"))
                .stock(20)
                .weight(new BigDecimal("0.25"))
                .imageUrl("https://example.com/tshirt-blk.jpg")
                .variantOptions(List.of(colorOption, sizeOption))
                .build();
    }

    private ProductVariantResponse createMockResponse() {
        VariantOptionResponse voColor = VariantOptionResponse.builder()
                .id(1L)
                .optionId(1L)
                .optionName("Color")
                .value("Black")
                .build();

        VariantOptionResponse voSize = VariantOptionResponse.builder()
                .id(2L)
                .optionId(2L)
                .optionName("Size")
                .value("L")
                .build();

        return ProductVariantResponse.builder()
                .id(100L)
                .productId(1L)
                .productName("Basic T-Shirt")
                .sku("TSHIRT-BLK-L")
                .price(new BigDecimal("150000.00"))
                .stock(20)
                .weight(new BigDecimal("0.25"))
                .imageUrl("https://example.com/tshirt-blk.jpg")
                .optionValues(List.of(voColor, voSize))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== POST /api/admin/products/{productId}/variants =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products/{productId}/variants should return 201 when request is valid")
    void addVariantToProduct_Success() throws Exception {
        ProductVariantRequest request = createValidRequest();
        ProductVariantResponse response = createMockResponse();

        when(productVariantService.addVariantToProduct(eq(1L), any(ProductVariantRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/products/1/variants").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product variant added successfully"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.sku").value("TSHIRT-BLK-L"))
                .andExpect(jsonPath("$.data.price").value(150000.00))
                .andExpect(jsonPath("$.data.stock").value(20));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products/{productId}/variants should return 400 when required fields are missing")
    void addVariantToProduct_MissingRequiredFields() throws Exception {
        ProductVariantRequest request = new ProductVariantRequest();

        mockMvc.perform(post("/api/admin/products/1/variants").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.sku").value("SKU is required"))
                .andExpect(jsonPath("$.data.price").value("Price is required"))
                .andExpect(jsonPath("$.data.stock").value("Stock is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products/{productId}/variants should return 404 when product not found")
    void addVariantToProduct_ProductNotFound() throws Exception {
        ProductVariantRequest request = createValidRequest();

        when(productVariantService.addVariantToProduct(eq(99L), any(ProductVariantRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(post("/api/admin/products/99/variants").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product not found with id : '99'"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/products/{productId}/variants should return 409 when SKU already exists")
    void addVariantToProduct_DuplicateSku() throws Exception {
        ProductVariantRequest request = createValidRequest();

        when(productVariantService.addVariantToProduct(eq(1L), any(ProductVariantRequest.class)))
                .thenThrow(new ConflictException("Product variant with SKU 'TSHIRT-BLK-L' already exists"));

        mockMvc.perform(post("/api/admin/products/1/variants").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product variant with SKU 'TSHIRT-BLK-L' already exists"));
    }

    // ===== GET /api/admin/products/{productId}/variants =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/products/{productId}/variants should return 200 with list of variants")
    void getVariantsByProductId_Success() throws Exception {
        ProductVariantResponse response = createMockResponse();

        when(productVariantService.getVariantsByProductId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/products/1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product variants fetched successfully"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sku").value("TSHIRT-BLK-L"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/products/{productId}/variants should return 404 when product not found")
    void getVariantsByProductId_ProductNotFound() throws Exception {
        when(productVariantService.getVariantsByProductId(99L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/admin/products/99/variants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== PUT /api/admin/variants/{id} =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/variants/{id} should return 200 when variant and stock are updated")
    void updateVariant_Success() throws Exception {
        ProductVariantRequest request = createValidRequest();
        request.setStock(50);
        request.setPrice(new BigDecimal("160000.00"));

        ProductVariantResponse response = createMockResponse();
        response.setStock(50);
        response.setPrice(new BigDecimal("160000.00"));

        when(productVariantService.updateVariant(eq(100L), any(ProductVariantRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/variants/100").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product variant updated successfully"))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.price").value(160000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/variants/{id} should return 400 when stock is negative")
    void updateVariant_NegativeStock() throws Exception {
        ProductVariantRequest request = createValidRequest();
        request.setStock(-10);

        mockMvc.perform(put("/api/admin/variants/100").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.stock").value("Stock must be greater than or equal to 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/variants/{id} should return 404 when variant not found")
    void updateVariant_NotFound() throws Exception {
        ProductVariantRequest request = createValidRequest();

        when(productVariantService.updateVariant(eq(999L), any(ProductVariantRequest.class)))
                .thenThrow(new ResourceNotFoundException("ProductVariant", "id", 999L));

        mockMvc.perform(put("/api/admin/variants/999").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== DELETE /api/admin/variants/{id} =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/variants/{id} should return 200 when variant is deleted")
    void deleteVariant_Success() throws Exception {
        doNothing().when(productVariantService).deleteVariant(100L);

        mockMvc.perform(delete("/api/admin/variants/100").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product variant deleted successfully"));

        verify(productVariantService, times(1)).deleteVariant(100L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/variants/{id} should return 400 when deleting last variant of product")
    void deleteVariant_LastVariant() throws Exception {
        doThrow(new BadRequestException("Cannot delete the last variant of an active product"))
                .when(productVariantService).deleteVariant(100L);

        mockMvc.perform(delete("/api/admin/variants/100").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete the last variant of an active product"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/variants/{id} should return 409 when variant is used in orders")
    void deleteVariant_AssociatedWithOrder() throws Exception {
        doThrow(new ConflictException("Cannot delete variant because it is associated with existing orders"))
                .when(productVariantService).deleteVariant(100L);

        mockMvc.perform(delete("/api/admin/variants/100").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete variant because it is associated with existing orders"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/variants/{id} should return 404 when variant not found")
    void deleteVariant_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("ProductVariant", "id", 999L))
                .when(productVariantService).deleteVariant(999L);

        mockMvc.perform(delete("/api/admin/variants/999").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ===== SECURITY (ROLE_ADMIN) =====

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Admin variant endpoints should return 403 Forbidden when accessed by non-ADMIN user")
    void adminVariantEndpoints_ForbiddenForCustomer() throws Exception {
        ProductVariantRequest request = createValidRequest();

        mockMvc.perform(post("/api/admin/products/1/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/products/1/variants"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/variants/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/variants/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin variant endpoints should return 403 Forbidden when accessed without authentication")
    void adminVariantEndpoints_ForbiddenWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/products/1/variants"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/variants/100"))
                .andExpect(status().isForbidden());
    }
}

