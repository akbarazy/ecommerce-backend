package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.CartItemRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateCartItemRequest;
import com.akbarazy.ecommercebackend.dto.response.CartItemResponse;
import com.akbarazy.ecommercebackend.dto.response.CartResponse;
import com.akbarazy.ecommercebackend.entity.CartItem;
import com.akbarazy.ecommercebackend.entity.Product;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.CartItemRepository;
import com.akbarazy.ecommercebackend.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    @Transactional
    public CartItemResponse addToCart(String email, CartItemRequest request) {
        User user = getUserByEmail(email);
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId().toString()));
        
        if (product.getStock() < request.getQuantity()) {
            throw new BadRequestException("Stok tidak mencukupi");
        }
        
        Optional<CartItem> existingItemOptional = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        
        CartItem savedCartItem;
        if (existingItemOptional.isPresent()) {
            CartItem existingItem = existingItemOptional.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            savedCartItem = cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                .user(user)
                .product(product)
                .quantity(request.getQuantity())
                .build();
            savedCartItem = cartItemRepository.save(newItem);
        }
        
        return CartItemResponse.from(savedCartItem);
    }

    @Override
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
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId.toString()));
                
        if (cartItem.getProduct().getStock() < request.getQuantity()) {
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
            .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId.toString()));
                
        cartItemRepository.delete(cartItem);
    }
}
