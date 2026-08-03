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

import com.akbarazy.ecommercebackend.dto.request.LoginRequest;
import com.akbarazy.ecommercebackend.dto.response.AuthResponse;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import com.akbarazy.ecommercebackend.entity.BlacklistedToken;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest createRegisterRequest() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Akbarazy");
        registerRequest.setEmail("akbarazy@example.com");
        registerRequest.setPassword("#Password123");
        return registerRequest;
    }

    @Test
    @DisplayName("Register should succeed and return user response when request is valid")
    void registerValidRequest() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded#Password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse userResponse = authService.register(registerRequest);

        assertNotNull(userResponse);
        assertEquals(registerRequest.getName(), userResponse.getName());
        assertEquals(registerRequest.getEmail(), userResponse.getEmail());
        assertEquals(Role.USER, userResponse.getRole());
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register should execute dependencies in correct order")
    void registerCallDependenciesInCorrectOrder() {
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

        assertEquals("Email is already registered", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register should encode password before saving new user data")
    void registerEncodePassword() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded#Password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        authService.register(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("encoded#Password123", capturedUser.getPassword());
    }

    @Test
    @DisplayName("Register should set default user role to each new user")
    void registerDefaultUserRole() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded#Password123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        
        authService.register(registerRequest);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        
        assertEquals(Role.USER, capturedUser.getRole());
    }

    private LoginRequest createLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("akbarazy@example.com");
        loginRequest.setPassword("#Password123");
        return loginRequest;
    }

    private User createUser() {
        return User.builder()
            .id(1L)
            .name("Akbarazy")
            .email("akbarazy@example.com")
            .password("encoded#Password123")
            .role(Role.USER)
            .build();
    }

    @Test
    @DisplayName("Login should succeed and return auth response with token when request is valid")
    void loginValidRequest() {
        LoginRequest loginRequest = createLoginRequest();
        User user = createUser();
        String expectedToken = "jwt.token.here";

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getEmail())).thenReturn(expectedToken);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertNotNull(response.getUser());
        assertEquals(user.getEmail(), response.getUser().getEmail());
        assertEquals(user.getName(), response.getUser().getName());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verify(jwtTokenProvider).generateToken(user.getEmail());
    }

    @Test
    @DisplayName("Login should execute dependencies in correct order")
    void loginCallDependenciesInCorrectOrder() {
        LoginRequest loginRequest = createLoginRequest();
        User user = createUser();
        String expectedToken = "jwt.token.here";

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getEmail())).thenReturn(expectedToken);

        authService.login(loginRequest);

        InOrder inOrder = inOrder(userRepository, passwordEncoder, jwtTokenProvider);
        inOrder.verify(userRepository).findByEmail(loginRequest.getEmail());
        inOrder.verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        inOrder.verify(jwtTokenProvider).generateToken(user.getEmail());
    }

    @Test
    @DisplayName("Login should return response with bearer token type")
    void loginResponseContainsBearerTokenType() {
        LoginRequest loginRequest = createLoginRequest();
        User user = createUser();
        
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getEmail())).thenReturn("jwt.token.here");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("Login should throw bad credentials exception when email is not registered")
    void loginEmailNotRegistered() {
        LoginRequest loginRequest = createLoginRequest();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Login should throw bad credentials exception when password is incorrect")
    void loginIncorrectPassword() {
        LoginRequest loginRequest = createLoginRequest();
        User user = createUser();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verify(jwtTokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Logout should call dependencies in correct order")
    void logoutCallDependenciesInCorrectOrder() {
        String token = "jwt.token.here";
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        when(jwtTokenProvider.getExpirationFromToken(token)).thenReturn(expiryDate);
        when(blacklistedTokenRepository.save(any(BlacklistedToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        authService.logout(token);

        InOrder inOrder = inOrder(jwtTokenProvider, blacklistedTokenRepository);
        inOrder.verify(jwtTokenProvider).getExpirationFromToken(token);
        inOrder.verify(blacklistedTokenRepository).save(any(BlacklistedToken.class));
    }

    @Test
    @DisplayName("Logout should save blacklisted token to repository")
    void logoutSaveBlacklistedToken() {
        String token = "jwt.token.here";
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        when(jwtTokenProvider.getExpirationFromToken(token)).thenReturn(expiryDate);
        when(blacklistedTokenRepository.save(any(BlacklistedToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        authService.logout(token);

        verify(jwtTokenProvider).getExpirationFromToken(token);
        verify(blacklistedTokenRepository).save(any(BlacklistedToken.class));
    }

    @Test
    @DisplayName("Logout should save correct token value and expiry date")
    void logoutSaveCorrectTokenAndExpiryDate() {
        String token = "jwt.token.here";
        LocalDateTime expiryDate = LocalDateTime.of(2026, 7, 31, 12, 0, 0);

        when(jwtTokenProvider.getExpirationFromToken(token)).thenReturn(expiryDate);
        when(blacklistedTokenRepository.save(any(BlacklistedToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        authService.logout(token);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());
        BlacklistedToken captured = captor.getValue();

        assertEquals(token, captured.getToken());
        assertEquals(expiryDate, captured.getExpiryDate());
    }
}