package com.akbarazy.ecommercebackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {
    private Long productId;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    private Integer stock;

    @DecimalMin(value = "0.0", inclusive = true, message = "Weight must be greater than or equal to 0")
    private BigDecimal weight;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Valid
    @Builder.Default
    private List<VariantOptionRequest> optionValues = new ArrayList<>();

    public List<VariantOptionRequest> getOptions() {
        return optionValues;
    }

    public void setOptions(List<VariantOptionRequest> options) {
        this.optionValues = options;
    }

    public List<VariantOptionRequest> getValues() {
        return optionValues;
    }

    public void setValues(List<VariantOptionRequest> values) {
        this.optionValues = values;
    }
}