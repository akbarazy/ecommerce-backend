package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.RegisterRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.entity.enums.Role;
import com.akbarazy.ecommercebackend.exception.BadRequestException;
import com.akbarazy.ecommercebackend.exception.GlobalExceptionHandler;
import com.akbarazy.ecommercebackend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    // ===== Helper Methods =====

    private String createRequestJson(String name, String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return objectMapper.writeValueAsString(request);
    }

    private UserResponse createUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== Success Cases =====

    @Test
    @DisplayName("POST /api/auth/register - should return 201 Created on valid input")
    void register_ShouldReturn201_WhenInputIsValid() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(createUserResponse());

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("Test User", "test@example.com", "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ===== Validation Error Cases =====

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when name is blank")
    void register_ShouldReturn400_WhenNameIsBlank() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("", "test@example.com", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Name is required"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when email is blank")
    void register_ShouldReturn400_WhenEmailIsBlank() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("Test User", "", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when email is invalid")
    void register_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("Test User", "not-an-email", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").value("Email must be valid"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when password is blank")
    void register_ShouldReturn400_WhenPasswordIsBlank() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("Test User", "test@example.com", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when password is less than 8 characters")
    void register_ShouldReturn400_WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("Test User", "test@example.com", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.password").value("Password must be at least 8 characters"));
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 with multiple errors when all fields are blank")
    void register_ShouldReturn400_WhenAllFieldsAreBlank() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("", "", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when request body is missing")
    void register_ShouldReturn400_WhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request body is missing or unreadable"));
    }

    // ===== Business Logic Error Cases =====

    @Test
    @DisplayName("POST /api/auth/register - should return 400 when email is already registered")
    void register_ShouldReturn400_WhenEmailAlreadyExists() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Email is already registered"));

        // Act & Assert
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson("Test User", "existing@example.com", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }
}
