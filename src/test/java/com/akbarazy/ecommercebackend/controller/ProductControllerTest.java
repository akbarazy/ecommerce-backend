package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.response.ProductOptionResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductResponse;
import com.akbarazy.ecommercebackend.dto.response.ProductVariantResponse;
import com.akbarazy.ecommercebackend.dto.response.VariantOptionResponse;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.service.ProductService;
import com.akbarazy.ecommercebackend.service.ProductVariantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductVariantService productVariantService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private ProductVariantResponse createMockVariantResponse(Long id, Long productId, String productName, String sku, BigDecimal price) {
        VariantOptionResponse voColor = VariantOptionResponse.builder()
                .id(1L)
                .optionId(1L)
                .optionName("Color")
                .value("Black")
                .build();

        return ProductVariantResponse.builder()
                .id(id)
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .price(price)
                .stock(10)
                .weight(new BigDecimal("1.50"))
                .optionValues(List.of(voColor))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ProductResponse createMockProductResponse(Long id, String name, BigDecimal price, String categoryName) {
        ProductOptionResponse option = ProductOptionResponse.builder()
                .id(1L)
                .productId(id)
                .name("Color")
                .values(List.of("Black"))
                .build();

        ProductVariantResponse variant = createMockVariantResponse(10L, id, name, "SKU-" + id, price);

        return ProductResponse.builder()
                .id(id)
                .name(name)
                .description("A great product")
                .categoryId(1L)
                .categoryName(categoryName)
                .options(List.of(option))
                .variants(List.of(variant))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== GET ALL PRODUCTS =====

    @Test
    @DisplayName("GET /api/products should return 200 with paginated products")
    void getAllProducts_Success() throws Exception {
        ProductResponse product1 = createMockProductResponse(1L, "Laptop", new BigDecimal("15000000.00"), "Electronics");
        ProductResponse product2 = createMockProductResponse(2L, "Headphone", new BigDecimal("500000.00"), "Electronics");

        Page<ProductResponse> page = new PageImpl<>(Arrays.asList(product1, product2), PageRequest.of(0, 10), 2);

        when(productService.getAllProducts(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Products fetched successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.data.content[0].variants[0].price").value(15000000.00))
                .andExpect(jsonPath("$.data.content[1].name").value("Headphone"));
    }

    @Test
    @DisplayName("GET /api/products should return 200 with empty page when no products exist")
    void getAllProducts_EmptyPage() throws Exception {
        Page<ProductResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(productService.getAllProducts(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/products?search=laptop should pass search parameter to service")
    void getAllProducts_WithSearchParam() throws Exception {
        ProductResponse product = createMockProductResponse(1L, "Gaming Laptop", new BigDecimal("15000000.00"), "Electronics");
        Page<ProductResponse> page = new PageImpl<>(Collections.singletonList(product), PageRequest.of(0, 10), 1);

        when(productService.getAllProducts(eq("laptop"), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/products")
                .param("search", "laptop")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Gaming Laptop"));
    }

    @Test
    @DisplayName("GET /api/products?categoryId=1&minPrice=1000000&maxPrice=20000000 should pass filter parameters")
    void getAllProducts_WithFilters() throws Exception {
        ProductResponse product = createMockProductResponse(1L, "Laptop", new BigDecimal("15000000.00"), "Electronics");
        Page<ProductResponse> page = new PageImpl<>(Collections.singletonList(product), PageRequest.of(0, 10), 1);

        when(productService.getAllProducts(isNull(), eq(1L), eq(new BigDecimal("1000000")), eq(new BigDecimal("20000000")), any())).thenReturn(page);

        mockMvc.perform(get("/api/products")
                .param("categoryId", "1")
                .param("minPrice", "1000000")
                .param("maxPrice", "20000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/products should return pagination metadata")
    void getAllProducts_PaginationMetadata() throws Exception {
        ProductResponse product = createMockProductResponse(1L, "Laptop", new BigDecimal("15000000.00"), "Electronics");
        Page<ProductResponse> page = new PageImpl<>(Collections.singletonList(product), PageRequest.of(0, 10), 25);

        when(productService.getAllProducts(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    // ===== GET PRODUCT BY ID =====

    @Test
    @DisplayName("GET /api/products/{id} should return 200 with product details and variants")
    void getProductById_Success() throws Exception {
        ProductResponse response = createMockProductResponse(1L, "Laptop", new BigDecimal("15000000.00"), "Electronics");

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Laptop"))
                .andExpect(jsonPath("$.data.categoryName").value("Electronics"))
                .andExpect(jsonPath("$.data.variants[0].price").value(15000000.00));
    }

    @Test
    @DisplayName("GET /api/products/{id} should return 404 when product not found")
    void getProductById_NotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/products/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product not found with id : '99'"));
    }

    @Test
    @DisplayName("GET /api/products/{id} should return complete response structure")
    void getProductById_ResponseStructure() throws Exception {
        ProductResponse response = createMockProductResponse(1L, "Laptop", new BigDecimal("15000000.00"), "Electronics");

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.description").exists())
                .andExpect(jsonPath("$.data.options").isArray())
                .andExpect(jsonPath("$.data.variants").isArray())
                .andExpect(jsonPath("$.data.categoryId").exists())
                .andExpect(jsonPath("$.data.categoryName").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    // ===== GET PRODUCT VARIANTS =====

    @Test
    @DisplayName("GET /api/products/{id}/variants should return 200 with product variants")
    void getProductVariants_Success() throws Exception {
        ProductVariantResponse variant = createMockVariantResponse(10L, 1L, "Laptop", "SKU-1", new BigDecimal("15000000.00"));

        when(productVariantService.getVariantsByProductId(1L)).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/products/1/variants")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product variants fetched successfully"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-1"))
                .andExpect(jsonPath("$.data[0].price").value(15000000.00));
    }

    @Test
    @DisplayName("GET /api/products/{id}/variants should return 404 when product not found")
    void getProductVariants_ProductNotFound() throws Exception {
        when(productVariantService.getVariantsByProductId(99L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/products/99/variants")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/products/variants/{variantId} should return 200 with variant details")
    void getVariantById_Success() throws Exception {
        ProductVariantResponse variant = createMockVariantResponse(10L, 1L, "Laptop", "SKU-1", new BigDecimal("15000000.00"));

        when(productVariantService.getVariantById(10L)).thenReturn(variant);

        mockMvc.perform(get("/api/products/variants/10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product variant fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.sku").value("SKU-1"));
    }
}
