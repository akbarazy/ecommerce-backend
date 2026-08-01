package com.akbarazy.ecommercebackend.service;

import com.akbarazy.ecommercebackend.dto.request.ChangePasswordRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateProfileRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;

public interface UserService {
    UserResponse getProfile(String email);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
}