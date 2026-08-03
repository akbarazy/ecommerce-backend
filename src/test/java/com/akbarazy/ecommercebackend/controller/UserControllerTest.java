package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.request.ChangePasswordRequest;
import com.akbarazy.ecommercebackend.dto.request.UpdateProfileRequest;
import com.akbarazy.ecommercebackend.dto.response.UserResponse;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "budi@gmail.com")
    void getProfile_Success() throws Exception {
        UserResponse mockResponse = UserResponse.builder()
            .name("Budi Santoso")
            .email("budi@gmail.com")
            .build();

        when(userService.getProfile("budi@gmail.com")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile fetched successfully"))
                .andExpect(jsonPath("$.data.name").value("Budi Santoso"))
                .andExpect(jsonPath("$.data.email").value("budi@gmail.com"));
    }

    @Test
    @WithMockUser(username = "budi@gmail.com")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Budi Update");
        request.setPhone("0899999999");
        request.setAddress("Jalan Melati");

        UserResponse mockResponse = UserResponse.builder()
            .name("Budi Update")
            .email("budi@gmail.com")
            .build();
        
        when(userService.updateProfile(eq("budi@gmail.com"), any(UpdateProfileRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/api/users/me").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Budi Update"));
    }

    @Test
    @WithMockUser(username = "budi@gmail.com")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword456");

        mockMvc.perform(put("/api/users/me/password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile changed successfully"));
    }
}
