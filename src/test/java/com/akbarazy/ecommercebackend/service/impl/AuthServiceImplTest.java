package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.entity.enums.Role;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    // ===== Helper Methods =====

    private RegisterRequest createRegisterRequest(String name, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User createSavedUser(Long id, String name, String email, String encodedPassword) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .password(encodedPassword)
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ===== Success Cases =====

    @Test
    @DisplayName("Register - should register a new user successfully")
    void register_ShouldReturnUserResponse_WhenEmailIsNew() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "test@example.com", "password123");
        String encodedPassword = "$2a$10$encodedPassword";
        User savedUser = createSavedUser(1L, "Test User", "test@example.com", encodedPassword);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertNull(response.getPhone());
        assertNull(response.getAddress());
    }

    @Test
    @DisplayName("Register - should encode the password before saving")
    void register_ShouldEncodePassword_BeforeSaving() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "test@example.com", "password123");
        String encodedPassword = "$2a$10$encodedPassword";
        User savedUser = createSavedUser(1L, "Test User", "test@example.com", encodedPassword);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.register(request);

        // Assert
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals(encodedPassword)
        ));
    }

    @Test
    @DisplayName("Register - should set role to USER by default")
    void register_ShouldSetRoleToUser_ByDefault() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "test@example.com", "password123");
        User savedUser = createSavedUser(1L, "Test User", "test@example.com", "encoded");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.register(request);

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.USER
        ));
    }

    @Test
    @DisplayName("Register - should not expose password in response")
    void register_ShouldNotExposePassword_InResponse() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "test@example.com", "password123");
        User savedUser = createSavedUser(1L, "Test User", "test@example.com", "encoded");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = authService.register(request);

        // Assert - UserResponse does not have a password field, confirming it's not exposed
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
    }

    // ===== Failure Cases =====

    @Test
    @DisplayName("Register - should throw BadRequestException when email is already registered")
    void register_ShouldThrowBadRequestException_WhenEmailExists() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "existing@example.com", "password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is already registered", exception.getMessage());
    }

    @Test
    @DisplayName("Register - should not encode password when email already exists")
    void register_ShouldNotEncodePassword_WhenEmailExists() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "existing@example.com", "password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.register(request));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== Interaction Verification =====

    @Test
    @DisplayName("Register - should call repository methods in the correct order")
    void register_ShouldCallRepositoryMethods_InCorrectOrder() {
        // Arrange
        RegisterRequest request = createRegisterRequest("Test User", "test@example.com", "password123");
        User savedUser = createSavedUser(1L, "Test User", "test@example.com", "encoded");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        authService.register(request);

        // Assert - verify the order: check email first, then save
        var inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).existsByEmail("test@example.com");
        inOrder.verify(passwordEncoder).encode("password123");
        inOrder.verify(userRepository).save(any(User.class));
    }
}
