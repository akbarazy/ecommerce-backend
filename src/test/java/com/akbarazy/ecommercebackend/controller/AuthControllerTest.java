package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.entity.enums.Role;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private static final String REGISTER_URL = "/api/auth/register";

    private RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Akbarazy");
        request.setEmail("akbarazy@example.com");
        request.setPassword("#password123");
        return request;
    }

    private UserResponse createUserResponse() {
        return UserResponse.builder()
            .id(1L)
            .name("Akbarazy")
            .email("akbarazy@example.com")
            .phone(null)
            .address(null)
            .role(Role.USER)
            .createdAt(LocalDateTime.now())
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
}