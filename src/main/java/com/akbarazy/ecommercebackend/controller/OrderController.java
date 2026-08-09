package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.CheckoutRequest;
import com.akbarazy.ecommercebackend.dto.response.ApiResponse;
import com.akbarazy.ecommercebackend.dto.response.OrderResponse;
import com.akbarazy.ecommercebackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Checkout berhasil", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            Authentication authentication,
            Pageable pageable) {
        Page<OrderResponse> response = orderService.getMyOrders(authentication.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil daftar pesanan", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrderById(
            Authentication authentication,
            @PathVariable Long orderId) {
        OrderResponse response = orderService.getMyOrderById(authentication.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil detail pesanan", response));
    }
}
