package com.akbarazy.ecommercebackend.controller;

import com.akbarazy.ecommercebackend.dto.response.CategoryResponse;
import com.akbarazy.ecommercebackend.security.CustomUserDetailsService;
import com.akbarazy.ecommercebackend.security.JwtTokenProvider;
import com.akbarazy.ecommercebackend.repository.BlacklistedTokenRepository;
import com.akbarazy.ecommercebackend.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private CategoryResponse createMockCategoryResponse(Long id, String name, String description, int productCount) {
        return CategoryResponse.builder()
                .id(id)
                .name(name)
                .description(description)
                .productCount(productCount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/categories should return 200 with list of categories")
    void getAllCategories_Success() throws Exception {
        CategoryResponse category1 = createMockCategoryResponse(1L, "Electronics", "Electronic items", 5);
        CategoryResponse category2 = createMockCategoryResponse(2L, "Fashion", "Fashion items", 3);

        when(categoryService.getAllCategories()).thenReturn(Arrays.asList(category1, category2));

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Categories fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[1].name").value("Fashion"));
    }

    @Test
    @DisplayName("GET /api/categories should return 200 with empty list when no categories exist")
    void getAllCategories_EmptyList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/categories should return complete response structure")
    void getAllCategories_ResponseStructure() throws Exception {
        CategoryResponse category = createMockCategoryResponse(1L, "Electronics", "Electronic items", 5);

        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category));

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[0].description").value("Electronic items"))
                .andExpect(jsonPath("$.data[0].productCount").value(5))
                .andExpect(jsonPath("$.data[0].createdAt").exists());
    }
}
