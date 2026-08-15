package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.dto.request.LoginRequest;
import com.akbarazy.ecommercebackend.dto.response.AuthResponse;
import com.akbarazy.ecommercebackend.entity.enums.UserRole;
import com.akbarazy.ecommercebackend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.BadCredentialsException;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String LOGOUT_URL = "/api/auth/logout";

    private RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Akbarazy");
        request.setEmail("akbarazy@example.com");
        request.setPassword("#Password123");
        return request;
    }

    private UserResponse createUserResponse() {
        return UserResponse.builder()
            .id(1L)
            .name("Akbarazy")
            .email("akbarazy@example.com")
            .phone(null)
            .address(null)
            .role(UserRole.CUSTOMER)
            .build();
    }

    @Test
    @DisplayName("POST /api/auth/register should return 201 when request is valid")
    void registerValidRequest() throws Exception {
        RegisterRequest registerRequest = createRegisterRequest();
        UserResponse userResponse = createUserResponse();

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(
            post(REGISTER_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Registration successful"))
            .andExpect(jsonPath("$.data.email").value(registerRequest.getEmail())
        );
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 when request body is missing")
    void registerMissingBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is missing or unreadable"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 when each field is blank")
    void registerEachFieldBlank() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Name is required"))
                .andExpect(jsonPath("$.data.email").value("Email is required"))
                .andExpect(jsonPath("$.data.password").value("Password is required"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 when email is invalid")
    void registerInvalidEmail() throws Exception {
        RegisterRequest registerRequest = createRegisterRequest();
        registerRequest.setEmail("not-an-email");
        
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.email").value("Email must be valid"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 400 when password is too short")
    void registerShortPassword() throws Exception {
        RegisterRequest registerRequest = createRegisterRequest();
        registerRequest.setPassword("12345");
        
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.password").value("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return error when email already exists")
    void registerEmailAlreadyExists() throws Exception {
        RegisterRequest registerRequest = createRegisterRequest();
        
        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new com.akbarazy.ecommercebackend.exception.ConflictException("Email is already registered"));

        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    private LoginRequest createLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("akbarazy@example.com");
        request.setPassword("#Password123");
        return request;
    }

    private AuthResponse createAuthResponse() {
        return AuthResponse.builder()
            .token("jwt-token-xyz-123")
            .tokenType("Bearer")
            .user(createUserResponse())
            .build();
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 with auth token when request is valid")
    void loginValidRequest() throws Exception {
        LoginRequest loginRequest = createLoginRequest();
        AuthResponse authResponse = createAuthResponse();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(
            post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Login successful"))
            .andExpect(jsonPath("$.data.token").value(authResponse.getToken()))
            .andExpect(jsonPath("$.data.tokenType").value(authResponse.getTokenType()))
            .andExpect(jsonPath("$.data.user.email").value(loginRequest.getEmail()));
    }
    @Test
    @DisplayName("POST /api/auth/login should return 400 when request body is missing")
    void loginMissingBody() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is missing or unreadable"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 400 when each field is blank")
    void loginEachFieldBlank() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.data.email").value("Email is required"))
            .andExpect(jsonPath("$.data.password").value("Password is required"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 400 when email is invalid")
    void loginInvalidEmail() throws Exception {
        LoginRequest loginRequest = createLoginRequest();
        loginRequest.setEmail("not-an-email");
        
        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.data.email").value("Email must be valid"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 when credentials are invalid")
    void loginInvalidCredentials() throws Exception {
        LoginRequest loginRequest = createLoginRequest();
        
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return complete response structure")
    void loginResponseStructure() throws Exception {
        LoginRequest loginRequest = createLoginRequest();
        AuthResponse authResponse = createAuthResponse();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(
            post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.tokenType").exists())
            .andExpect(jsonPath("$.data.user.id").exists())
            .andExpect(jsonPath("$.data.user.name").exists())
            .andExpect(jsonPath("$.data.user.email").exists())
            .andExpect(jsonPath("$.data.user.role").exists());
    }

    @Test
    @DisplayName("POST /api/auth/logout should return 200 when token is provided")
    void logoutValidToken() throws Exception {
        String token = "jwt-token-xyz-123";

        doNothing().when(authService).logout(token);

        mockMvc.perform(post(LOGOUT_URL)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logout(token);
    }

    @Test
    @DisplayName("POST /api/auth/logout should return 200 even without authorization header")
    void logoutNoAuthorizationHeader() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService, never()).logout(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/logout should pass correct token to service")
    void logoutCallServiceCorrectToken() throws Exception {
        String token = "specific-jwt-token-abc";

        doNothing().when(authService).logout(token);

        mockMvc.perform(post(LOGOUT_URL)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        verify(authService).logout(token);
    }
}