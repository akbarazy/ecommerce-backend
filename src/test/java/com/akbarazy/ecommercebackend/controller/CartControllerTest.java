package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.response.CartItemResponse;
import com.akbarazy.ecommercebackend.dto.response.CartResponse;
import com.akbarazy.ecommercebackend.dto.response.VariantOptionResponse;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.service.CartService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private CartItemResponse createMockCartItemResponse() {
        VariantOptionResponse voSize = VariantOptionResponse.builder()
                .id(1L)
                .optionId(1L)
                .optionName("Size")
                .value("42")
                .build();

        return CartItemResponse.builder()
                .id(1L)
                .variantId(100L)
                .productId(10L)
                .productName("Sepatu Sneakers")
                .sku("SPK-42-BLK")
                .price(new BigDecimal("250000.00"))
                .variantOptions(List.of(voSize))
                .quantity(2)
                .subtotal(new BigDecimal("500000.00"))
                .build();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("GET /api/cart should return 200 with user cart")
    void getCart_Success() throws Exception {
        CartItemResponse item = createMockCartItemResponse();
        CartResponse cartResponse = CartResponse.builder()
                .items(List.of(item))
                .totalItems(1)
                .totalPrice(new BigDecimal("500000.00"))
                .build();

        when(cartService.getCart("user@example.com")).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart fetched successfully"))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalPrice").value(500000.00))
                .andExpect(jsonPath("$.data.items[0].variantId").value(100))
                .andExpect(jsonPath("$.data.items[0].sku").value("SPK-42-BLK"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("POST /api/cart/items should return 201 when variant is added to cart")
    void addToCart_Success() throws Exception {
        CartItemRequest request = CartItemRequest.builder()
                .variantId(100L)
                .quantity(2)
                .build();

        CartItemResponse response = createMockCartItemResponse();

        when(cartService.addToCart(eq("user@example.com"), any(CartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/cart/items").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added to cart"))
                .andExpect(jsonPath("$.data.variantId").value(100))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("POST /api/cart/items should return 400 when variantId or quantity is missing")
    void addToCart_ValidationFailed() throws Exception {
        CartItemRequest request = new CartItemRequest();

        mockMvc.perform(post("/api/cart/items").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.variantId").value("Variant ID wajib diisi"))
                .andExpect(jsonPath("$.data.quantity").value("Quantity wajib diisi"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("POST /api/cart/items should return 400 when variant stock is insufficient")
    void addToCart_InsufficientStock() throws Exception {
        CartItemRequest request = CartItemRequest.builder()
                .variantId(100L)
                .quantity(99)
                .build();

        when(cartService.addToCart(eq("user@example.com"), any(CartItemRequest.class)))
                .thenThrow(new BadRequestException("Stok tidak mencukupi"));

        mockMvc.perform(post("/api/cart/items").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Stok tidak mencukupi"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("PUT /api/cart/items/{itemId} should return 200 when quantity is updated")
    void updateCartItem_Success() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(5)
                .build();

        CartItemResponse response = createMockCartItemResponse();
        response.setQuantity(5);
        response.setSubtotal(new BigDecimal("1250000.00"));

        when(cartService.updateCartItem(eq("user@example.com"), eq(1L), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/cart/items/1").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart item updated successfully"))
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("DELETE /api/cart/items/{itemId} should return 200 when item is removed")
    void removeFromCart_Success() throws Exception {
        doNothing().when(cartService).removeFromCart("user@example.com", 1L);

        mockMvc.perform(delete("/api/cart/items/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from cart"));

        verify(cartService, times(1)).removeFromCart("user@example.com", 1L);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("DELETE /api/cart/items/{itemId} should return 404 when cart item not found")
    void removeFromCart_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("CartItem", "id", 99L))
                .when(cartService).removeFromCart("user@example.com", 99L);

        mockMvc.perform(delete("/api/cart/items/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}

