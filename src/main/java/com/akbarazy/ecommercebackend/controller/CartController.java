package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.response.ApiResponse;
import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.response.CartItemResponse;
import com.akbarazy.ecommercebackend.dto.response.CartResponse;
import com.akbarazy.ecommercebackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        String email = authentication.getName();
        CartResponse response = cartService.getCart(email);
        return new ResponseEntity<>(ApiResponse.success("Cart fetched successfully", response), HttpStatus.OK);
    }
    
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request
    ) {
        String email = authentication.getName();
        CartItemResponse response = cartService.addToCart(email, request);
        return new ResponseEntity<>(ApiResponse.success("Item added to cart", response), HttpStatus.CREATED);
    }
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        String email = authentication.getName();
        CartItemResponse response = cartService.updateCartItem(email, itemId, request);
        return new ResponseEntity<>(ApiResponse.success("Cart item updated successfully", response), HttpStatus.OK);
    }
    
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        String email = authentication.getName();
        cartService.removeFromCart(email, itemId);
        return new ResponseEntity<>(ApiResponse.success("Item removed from cart", null), HttpStatus.OK);
    }
}
