package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.CheckoutRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateOrderStatusRequest;
import com.akbarazy.ecommercebackend.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse checkout(String email, CheckoutRequest request);
    Page<OrderResponse> getMyOrders(String email, Pageable pageable);
    OrderResponse getMyOrderById(String email, Long orderId);
    Page<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse getOrderById(Long orderId);
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
}
