package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.enums.UserRole;
import com.akbarazy.ecommercebackend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private LocalDateTime createdAt;

    public static UserResponse register(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .address(user.getAddress())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}