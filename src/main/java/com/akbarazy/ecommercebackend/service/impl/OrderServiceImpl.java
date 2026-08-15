package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CheckoutRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateOrderStatusRequest;
import com.akbarazy.ecommercebackend.dto.response.OrderResponse;
import com.akbarazy.ecommercebackend.entity.CartItem;
import com.akbarazy.ecommercebackend.entity.Order;
import com.akbarazy.ecommercebackend.entity.OrderItem;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.entity.enums.OrderStatus;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CartItemRepository;
import com.akbarazy.ecommercebackend.repository.OrderRepository;
import com.akbarazy.ecommercebackend.repository.ProductRepository;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import com.akbarazy.ecommercebackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse checkout(String email, CheckoutRequest request) {
        User user = getUserByEmail(email);

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Keranjang kosong, tidak bisa checkout");
        }

        Order order = Order.builder()
            .user(user)
            .status(OrderStatus.PENDING)
            .shippingAddress(request.getShippingAddress())
            .orderItems(new ArrayList<>())
            .build();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException("Stok " + product.getName() + " tidak mencukupi");
            }

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(cartItem.getQuantity())
                .priceAtPurchase(product.getPrice())
                .build();

            order.getOrderItems().add(orderItem);
        }

        BigDecimal totalPrice = order.getOrderItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPrice(totalPrice);

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = UUID.randomUUID().toString().substring(0, 8);
        order.setOrderNumber("ORD-" + dateStr + "-" + randomStr);

        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteByUserId(user.getId());

        return OrderResponse.from(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return orderPage.map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(String email, Long orderId) {
        User user = getUserByEmail(email);
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status " + request.getStatus() + " tidak valid");
        }

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Tidak bisa mengubah status pesanan yang sudah " + currentStatus.name());
        }

        if (currentStatus == OrderStatus.PENDING && (newStatus != OrderStatus.PROCESSING && newStatus != OrderStatus.CANCELLED)) {
             throw new BadRequestException("Dari PENDING hanya bisa ke PROCESSING atau CANCELLED");
        }
        
        if (currentStatus == OrderStatus.PROCESSING && (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.CANCELLED)) {
             throw new BadRequestException("Dari PROCESSING hanya bisa ke SHIPPED atau CANCELLED");
        }
        
        if (currentStatus == OrderStatus.SHIPPED && newStatus != OrderStatus.DELIVERED) {
             throw new BadRequestException("Dari SHIPPED hanya bisa ke DELIVERED");
        }
    }
}
