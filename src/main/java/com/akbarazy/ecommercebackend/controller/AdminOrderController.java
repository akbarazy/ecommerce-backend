package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.UpdateOrderStatusRequest;
import com.akbarazy.ecommercebackend.dto.response.ApiResponse;
import com.akbarazy.ecommercebackend.dto.response.OrderResponse;
import com.akbarazy.ecommercebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(Pageable pageable) {
        Page<OrderResponse> response = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil semua pesanan", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil detail pesanan", response));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengupdate status pesanan", response));
    }
}
