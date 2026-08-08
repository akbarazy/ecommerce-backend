package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.response.CartItemResponse;
import com.akbarazy.ecommercebackend.dto.response.CartResponse;

public interface CartService {
    CartItemResponse addToCart(String email, CartItemRequest request);
    CartResponse getCart(String email);
    CartItemResponse updateCartItem(String email, Long itemId, UpdateCartItemRequest request);
    void removeFromCart(String email, Long itemId);
}
