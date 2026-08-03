package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.ChangePasswordRequest;
import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateProfileRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.ResourceNotFoundException;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .id(1L)
            .name("Budi Santoso")
            .email("budi@gmail.com")
            .password("encoded_password")
            .phone("08123456789")
            .address("Jalan Mawar")
            .build();
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findByEmail("budi@gmail.com")).thenReturn(Optional.of(mockUser));

        UserResponse response = userService.getProfile("budi@gmail.com");

        assertNotNull(response);
        assertEquals("Budi Santoso", response.getName());
        assertEquals("budi@gmail.com", response.getEmail());
        
        verify(userRepository, times(1)).findByEmail("budi@gmail.com");
    }

    @Test
    void getProfile_UserNotFound() {
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getProfile("unknown@gmail.com");
        });

        verify(userRepository, times(1)).findByEmail("unknown@gmail.com");
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Budi Update");
        request.setPhone("0899999999");
        request.setAddress("Jalan Melati");

        when(userRepository.findByEmail("budi@gmail.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile("budi@gmail.com", request);

        assertNotNull(response);
        assertEquals("Budi Update", response.getName());
        assertEquals("0899999999", response.getPhone());
        assertEquals("Jalan Melati", response.getAddress());

        verify(userRepository, times(1)).findByEmail("budi@gmail.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    private ChangePasswordRequest createChangePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old#Password123");
        request.setNewPassword("new#Password123");
        return request;
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword456");

        when(userRepository.findByEmail("budi@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("oldPassword123", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("new_encoded_password");

        assertDoesNotThrow(() -> userService.changePassword("budi@gmail.com", request));

        assertEquals("new_encoded_password", mockUser.getPassword());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void changePassword_WrongCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongOldPassword");
        request.setNewPassword("newPassword456");

        when(userRepository.findByEmail("budi@gmail.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongOldPassword", "encoded_password")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> {
            userService.changePassword("budi@gmail.com", request);
        });

        verify(userRepository, never()).save(any(User.class));
    }
}
