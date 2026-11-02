package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.response.CartItemResponse;
import com.akbarazy.ecommercebackend.dto.response.CartResponse;
import com.akbarazy.ecommercebackend.entity.CartItem;
import com.akbarazy.ecommercebackend.entity.ProductVariant;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CartItemRepository;
import com.akbarazy.ecommercebackend.repository.ProductVariantRepository;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import com.akbarazy.ecommercebackend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    @Transactional
    public CartItemResponse addToCart(String email, CartItemRequest request) {
        User user = getUserByEmail(email);
        
        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", request.getVariantId()));
        
        int availableStock = variant.getStock() != null ? variant.getStock() : 0;
        if (availableStock < request.getQuantity()) {
            throw new BadRequestException("Stok tidak mencukupi");
        }
        
        Optional<CartItem> existingItemOptional = cartItemRepository.findByUserIdAndVariantId(user.getId(), variant.getId());
        
        CartItem savedCartItem;
        if (existingItemOptional.isPresent()) {
            CartItem existingItem = existingItemOptional.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (availableStock < newQuantity) {
                throw new BadRequestException("Stok tidak mencukupi");
            }
            existingItem.setQuantity(newQuantity);
            savedCartItem = cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                .user(user)
                .variant(variant)
                .quantity(request.getQuantity())
                .build();
            savedCartItem = cartItemRepository.save(newItem);
        }
        
        return CartItemResponse.from(savedCartItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        User user = getUserByEmail(email);
        
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        
        List<CartItemResponse> itemResponses = cartItems.stream()
            .map(CartItemResponse::from)
            .collect(Collectors.toList());
        
        BigDecimal totalPrice = itemResponses.stream()
            .map(CartItemResponse::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        return CartResponse.builder()
            .items(itemResponses)
            .totalItems(itemResponses.size())
            .totalPrice(totalPrice)
            .build();
    }

    @Override
    @Transactional
    public CartItemResponse updateCartItem(String email, Long itemId, UpdateCartItemRequest request) {
        User user = getUserByEmail(email);
        
        CartItem cartItem = cartItemRepository.findByIdAndUserId(itemId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));
                
        ProductVariant variant = productVariantRepository.findById(cartItem.getVariant().getId())
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", cartItem.getVariant().getId()));

        int availableStock = variant.getStock() != null ? variant.getStock() : 0;
        if (availableStock < request.getQuantity()) {
            throw new BadRequestException("Stok tidak mencukupi");
        }
        
        cartItem.setQuantity(request.getQuantity());
        CartItem savedCartItem = cartItemRepository.save(cartItem);
        
        return CartItemResponse.from(savedCartItem);
    }

    @Override
    @Transactional
    public void removeFromCart(String email, Long itemId) {
        User user = getUserByEmail(email);
        
        CartItem cartItem = cartItemRepository.findByIdAndUserId(itemId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));
                
        cartItemRepository.delete(cartItem);
    }
}
