package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.entity.Role;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import com.akbarazy.ecommercebackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .build();
        User savedUser = userRepository.save(user);
        
        return UserResponse.register(savedUser);
    }
}