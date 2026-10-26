package com.akbarazy.ecommercebackend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantOptionRequest {
    private Long optionId;
    private String optionName;

    @NotBlank(message = "Option value is required")
    @Size(max = 100, message = "Option value must not exceed 100 characters")
    private String value;

    @JsonIgnore
    @AssertTrue(message = "Either optionId or optionName must be provided")
    public boolean isOptionIdentifierProvided() {
        return optionId != null || (optionName != null && !optionName.trim().isEmpty());
    }
}