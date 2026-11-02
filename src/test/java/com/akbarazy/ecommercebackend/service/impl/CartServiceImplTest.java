package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.response.CartItemResponse;
import com.akbarazy.ecommercebackend.dto.response.CartResponse;
import com.akbarazy.ecommercebackend.entity.CartItem;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CartItemRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User mockUser;
    private Product mockProduct;
    private ProductVariant mockVariant;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .build();

        mockProduct = Product.builder()
            .id(10L)
            .name("Sepatu Sneakers")
            .build();

        mockVariant = ProductVariant.builder()
            .id(100L)
            .sku("SPK-42-BLK")
            .price(new BigDecimal("250000.00"))
            .stock(10)
            .product(mockProduct)
            .values(new ArrayList<>())
            .build();
    }

    @Test
    @DisplayName("addToCart: Success when adding new item with sufficient stock")
    void addToCart_Success_NewItem() {
        CartItemRequest request = CartItemRequest.builder()
            .variantId(100L)
            .quantity(3)
            .build();

        CartItem savedItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(3)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.findByUserIdAndVariantId(1L, 100L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        CartItemResponse response = cartService.addToCart(mockUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getVariantId());
        assertEquals(3, response.getQuantity());
        assertEquals(new BigDecimal("750000.00"), response.getSubtotal());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addToCart: Success when updating existing item with sufficient stock")
    void addToCart_Success_ExistingItem() {
        CartItemRequest request = CartItemRequest.builder()
            .variantId(100L)
            .quantity(4)
            .build();

        CartItem existingItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        CartItem updatedItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(6)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.findByUserIdAndVariantId(1L, 100L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(existingItem)).thenReturn(updatedItem);

        CartItemResponse response = cartService.addToCart(mockUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(6, response.getQuantity());
        verify(cartItemRepository, times(1)).save(existingItem);
    }

    @Test
    @DisplayName("addToCart: Throws ResourceNotFoundException when variant not found")
    void addToCart_VariantNotFound() {
        CartItemRequest request = CartItemRequest.builder()
            .variantId(999L)
            .quantity(1)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(productVariantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            cartService.addToCart(mockUser.getEmail(), request)
        );
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addToCart: Throws BadRequestException when variant stock is insufficient for new item")
    void addToCart_InsufficientStock_NewItem() {
        mockVariant.setStock(2);
        CartItemRequest request = CartItemRequest.builder()
            .variantId(100L)
            .quantity(5)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            cartService.addToCart(mockUser.getEmail(), request)
        );
        assertEquals("Stok tidak mencukupi", ex.getMessage());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("addToCart: Throws BadRequestException when accumulated quantity exceeds variant stock")
    void addToCart_InsufficientStock_ExistingItem() {
        mockVariant.setStock(5);
        CartItem existingItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(3)
            .build();

        CartItemRequest request = CartItemRequest.builder()
            .variantId(100L)
            .quantity(3)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.findByUserIdAndVariantId(1L, 100L)).thenReturn(Optional.of(existingItem));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            cartService.addToCart(mockUser.getEmail(), request)
        );
        assertEquals("Stok tidak mencukupi", ex.getMessage());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCartItem: Success when updated quantity is within available stock")
    void updateCartItem_Success() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
            .quantity(5)
            .build();

        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        CartItem savedItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(5)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));
        when(cartItemRepository.save(cartItem)).thenReturn(savedItem);

        CartItemResponse response = cartService.updateCartItem(mockUser.getEmail(), 1L, request);

        assertNotNull(response);
        assertEquals(5, response.getQuantity());
        verify(cartItemRepository, times(1)).save(cartItem);
    }

    @Test
    @DisplayName("updateCartItem: Throws BadRequestException when updated quantity exceeds variant stock")
    void updateCartItem_InsufficientStock() {
        mockVariant.setStock(4);
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
            .quantity(7)
            .build();

        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            cartService.updateCartItem(mockUser.getEmail(), 1L, request)
        );
        assertEquals("Stok tidak mencukupi", ex.getMessage());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCartItem: Throws ResourceNotFoundException when cart item not found")
    void updateCartItem_CartItemNotFound() {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
            .quantity(2)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            cartService.updateCartItem(mockUser.getEmail(), 99L, request)
        );
    }

    @Test
    @DisplayName("getCart: Returns cart response with total price and items")
    void getCart_Success() {
        CartItem item1 = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(item1));

        CartResponse response = cartService.getCart(mockUser.getEmail());

        assertNotNull(response);
        assertEquals(1, response.getTotalItems());
        assertEquals(new BigDecimal("500000.00"), response.getTotalPrice());
    }

    @Test
    @DisplayName("removeFromCart: Successfully deletes cart item")
    void removeFromCart_Success() {
        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cartItem));

        cartService.removeFromCart(mockUser.getEmail(), 1L);

        verify(cartItemRepository, times(1)).delete(cartItem);
    }
}

