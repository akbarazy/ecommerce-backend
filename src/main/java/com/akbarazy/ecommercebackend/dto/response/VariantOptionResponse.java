package com.akbarazy.ecommercebackend.dto.response;

import com.akbarazy.ecommercebackend.entity.VariantOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantOptionResponse {
    private Long id;
    private Long optionId;
    private String optionName;
    private String value;

    public static VariantOptionResponse from(VariantOption variantOption) {
        if (variantOption == null) return null;
        return VariantOptionResponse.builder()
            .id(variantOption.getId())
            .optionId(variantOption.getProductOption() != null ? variantOption.getProductOption().getId() : null)
            .optionName(variantOption.getOptionName())
            .value(variantOption.getValue())
            .build();
    }
}