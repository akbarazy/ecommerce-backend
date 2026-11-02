package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CheckoutRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateOrderStatusRequest;
import com.akbarazy.ecommercebackend.dto.response.OrderResponse;
import com.akbarazy.ecommercebackend.entity.*;
import com.akbarazy.ecommercebackend.entity.enums.OrderStatus;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CartItemRepository;
import com.akbarazy.ecommercebackend.repository.OrderRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User mockUser;
    private Product mockProduct;
    private ProductOption sizeOption;
    private ProductVariant mockVariant;
    private VariantOption variantOption;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .id(1L)
            .name("Jane Doe")
            .email("jane@example.com")
            .build();

        mockProduct = Product.builder()
            .id(10L)
            .name("Kemeja Flanel")
            .build();

        sizeOption = ProductOption.builder()
            .id(1L)
            .name("Size")
            .product(mockProduct)
            .build();

        mockVariant = ProductVariant.builder()
            .id(100L)
            .sku("KMJ-FLN-L")
            .price(new BigDecimal("150000.00"))
            .stock(10)
            .product(mockProduct)
            .values(new ArrayList<>())
            .build();

        variantOption = VariantOption.builder()
            .id(1L)
            .productVariant(mockVariant)
            .productOption(sizeOption)
            .value("L")
            .build();

        mockVariant.getValues().add(variantOption);
    }

    @Test
    @DisplayName("checkout: Success when cart has items, stock is sufficient, and atomic decrement succeeds")
    void checkout_Success() {
        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("Jl. Jendral Sudirman No. 1, Jakarta");

        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.decrementStock(100L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(50L);
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        OrderResponse response = orderService.checkout(mockUser.getEmail(), request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(new BigDecimal("300000.00"), response.getTotalPrice());
        assertEquals(1, response.getItems().size());
        assertEquals("Kemeja Flanel", response.getItems().get(0).getProductName());
        assertEquals("Size: L", response.getItems().get(0).getVariantAttributes());
        assertEquals(2, response.getItems().get(0).getQuantity());

        // Verify atomic reduction called
        verify(productVariantRepository, times(1)).decrementStock(100L, 2);
        // Verify cart emptied
        verify(cartItemRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    @DisplayName("checkout: Throws BadRequestException when cart is empty")
    void checkout_EmptyCart() {
        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("Jl. Merdeka No. 5");

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            orderService.checkout(mockUser.getEmail(), request)
        );
        assertEquals("Keranjang kosong, tidak bisa checkout", ex.getMessage());
        verify(productVariantRepository, never()).decrementStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: Throws ResourceNotFoundException when variant does not exist")
    void checkout_VariantNotFound() {
        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("Jl. Sudirman No. 10");

        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(2)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            orderService.checkout(mockUser.getEmail(), request)
        );
        verify(productVariantRepository, never()).decrementStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("checkout: Throws BadRequestException when variant stock is less than cart quantity")
    void checkout_InsufficientStock_InitialValidation() {
        mockVariant.setStock(1);

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("Jl. Thamrin No. 20");

        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(3)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            orderService.checkout(mockUser.getEmail(), request)
        );
        assertEquals("Stok Kemeja Flanel tidak mencukupi", ex.getMessage());
        verify(productVariantRepository, never()).decrementStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout: Throws BadRequestException when atomic stock decrement fails due to race condition")
    void checkout_AtomicDecrementFails_ConcurrentUpdate() {
        mockVariant.setStock(5);

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("Jl. Gatot Subroto No. 3");

        CartItem cartItem = CartItem.builder()
            .id(1L)
            .user(mockUser)
            .variant(mockVariant)
            .quantity(3)
            .build();

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(cartItem));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.of(mockVariant));
        // Atomic decrement fails because concurrent thread took remaining stock
        when(productVariantRepository.decrementStock(100L, 3)).thenReturn(0);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
            orderService.checkout(mockUser.getEmail(), request)
        );
        assertEquals("Stok Kemeja Flanel tidak mencukupi", ex.getMessage());
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(anyLong());
    }

    @Test
    @DisplayName("getMyOrders: Returns page of orders for user")
    void getMyOrders_Success() {
        Order order = Order.builder()
            .id(1L)
            .orderNumber("ORD-20261102-12345678")
            .user(mockUser)
            .status(OrderStatus.PENDING)
            .shippingAddress("Jl. Test No. 1")
            .totalPrice(new BigDecimal("150000.00"))
            .createdAt(LocalDateTime.now())
            .build();

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(orderPage);

        Page<OrderResponse> responses = orderService.getMyOrders(mockUser.getEmail(), pageable);

        assertNotNull(responses);
        assertEquals(1, responses.getTotalElements());
    }

    @Test
    @DisplayName("updateOrderStatus: Success when transition is valid")
    void updateOrderStatus_Success() {
        Order order = Order.builder()
            .id(1L)
            .orderNumber("ORD-20261102-12345678")
            .user(mockUser)
            .status(OrderStatus.PENDING)
            .shippingAddress("Jl. Test No. 1")
            .totalPrice(new BigDecimal("150000.00"))
            .createdAt(LocalDateTime.now())
            .build();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("PROCESSING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertNotNull(response);
        assertEquals("PROCESSING", response.getStatus());
    }

    @Test
    @DisplayName("updateOrderStatus: Throws BadRequestException when transition is invalid")
    void updateOrderStatus_InvalidTransition() {
        Order order = Order.builder()
            .id(1L)
            .orderNumber("ORD-20261102-12345678")
            .user(mockUser)
            .status(OrderStatus.DELIVERED)
            .shippingAddress("Jl. Test No. 1")
            .totalPrice(new BigDecimal("150000.00"))
            .build();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("CANCELLED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () ->
            orderService.updateOrderStatus(1L, request)
        );
    }
}
