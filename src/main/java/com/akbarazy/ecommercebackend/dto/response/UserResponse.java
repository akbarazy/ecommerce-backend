package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.enums.UserRole;
import com.akbarazy.ecommercebackend.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private UserRole role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .address(user.getAddress())
            .role(user.getRole())
            .build();
    }
}