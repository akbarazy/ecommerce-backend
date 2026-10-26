package com.akbarazy.ecommercebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotEmpty(message = "Product options are required")
    @Valid
    @Builder.Default
    private List<ProductOptionRequest> options = new ArrayList<>();

    @NotEmpty(message = "Product variants are required")
    @Valid
    @Builder.Default
    private List<ProductVariantRequest> variants = new ArrayList<>();

    @JsonIgnore
    @AssertTrue(message = "Each variant must define values for all registered options")
    public boolean isVariantOptionsValid() {
        if (options == null || options.isEmpty() || variants == null || variants.isEmpty()) return true;
        Map<String, ProductOptionRequest> registeredOptionsMap = new HashMap<>();

        for (ProductOptionRequest option : options) {
            if (option == null || option.getName() == null || option.getName().trim().isEmpty()) return true;
            String normalizedName = option.getName().trim().toLowerCase();
            if (registeredOptionsMap.put(normalizedName, option) != null) return false;
        }

        if (registeredOptionsMap.isEmpty()) return true;

        for (ProductVariantRequest variant : variants) {
            if (variant == null) return false;
            List<VariantOptionRequest> variantOptions = variant.getVariantOptions();
            if (variantOptions == null || variantOptions.isEmpty()) return false;
            Set<String> definedOptionNames = new HashSet<>();

            for (VariantOptionRequest variantOption : variantOptions) {
                if (variantOption == null) return false;
                String optionName = variantOption.getOptionName();
                if (optionName == null || optionName.trim().isEmpty()) return false;
                String value = variantOption.getValue();
                if (value == null || value.trim().isEmpty()) return false;
                String normalizedName = optionName.trim().toLowerCase();
                ProductOptionRequest registeredOption = registeredOptionsMap.get(normalizedName);
                if (registeredOption == null) return false;
                if (!definedOptionNames.add(normalizedName)) return false;

                if (registeredOption.getValues() != null && !registeredOption.getValues().isEmpty()) {
                    boolean valueMatches = registeredOption.getValues().stream()
                        .filter(v -> v != null && !v.trim().isEmpty())
                        .anyMatch(v -> v.trim().equalsIgnoreCase(value.trim()));
                    if (!valueMatches) return false;
                }
            }

            if (!definedOptionNames.equals(registeredOptionsMap.keySet())) return false;
        }

        return true;
    }
}