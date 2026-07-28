package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.LoginRequest;
import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.ApiResponse;
import com.akbarazy.ecommercebackend.dto.response.AuthResponse;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return new ResponseEntity<>(ApiResponse.success("Registration successful", userResponse), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return new ResponseEntity<>(ApiResponse.success("Login successful", authResponse), HttpStatus.OK);
    }
}