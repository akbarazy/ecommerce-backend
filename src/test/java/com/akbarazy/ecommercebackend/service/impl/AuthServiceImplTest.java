package com.akbarazy.ecommercebackend.service.impl;

import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.entity.User;
import com.akbarazy.ecommercebackend.entity.enums.Role;
import com.akbarazy.ecommercebackend.exception.ConflictException;
import com.akbarazy.ecommercebackend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest createRegisterRequest() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Akbarazy");
        registerRequest.setEmail("akbarazy@example.com");
        registerRequest.setPassword("#password123");
        return registerRequest;
    }

    @Test
    @DisplayName("Register should succeed and return user response when request is valid")
    void registerValidRequest() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded#password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse userResponse = authService.register(registerRequest);

        assertNotNull(userResponse, "User response cannot be null");
        assertEquals(registerRequest.getName(), userResponse.getName(), "The name must match the register request");
        assertEquals(registerRequest.getEmail(), userResponse.getEmail(), "The email must match the register request");
        assertEquals(Role.USER, userResponse.getRole(), "The role must automatically become user");
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register should execute dependencies in correct order")
    void registerCallMethodsInCorrectOrder() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registerRequest);

        InOrder inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).existsByEmail(registerRequest.getEmail());
        inOrder.verify(passwordEncoder).encode(registerRequest.getPassword());
        inOrder.verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register should throw conflict exception when email already exists")
    void registerEmailAlreadyExists() {
        RegisterRequest registerRequest = createRegisterRequest();
        
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);
        
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email is already registered", exception.getMessage(), "The exception message doesn't match");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register should encode password before saving new user data")
    void registerEncodePassword() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded#password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("encoded#password123", capturedUser.getPassword(), "The password");
    }

    @Test
    @DisplayName("Register should set default user role to each new user")
    void registerDefaultUserRole() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded#password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        
        authService.register(registerRequest);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        
        assertEquals(Role.USER, capturedUser.getRole());
    }
}