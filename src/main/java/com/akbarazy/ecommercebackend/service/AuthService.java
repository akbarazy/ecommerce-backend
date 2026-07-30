package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.LoginRequest;
import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.AuthResponse;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
}