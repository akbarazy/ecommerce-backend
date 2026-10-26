package com.akbarazy.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionRequest {
    @NotBlank(message = "Option name is required")
    @Size(max = 100, message = "Option name must not exceed 100 characters")
    private String name;

    @Builder.Default
    private List<String> values = new ArrayList<>();
}