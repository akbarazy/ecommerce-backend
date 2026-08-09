package com.akbarazy.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    
    @NotBlank(message = "Status tidak boleh kosong")
    private String status;
}
