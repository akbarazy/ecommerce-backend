package com.akbarazy.ecommercebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {
    
    @NotBlank(message = "Alamat pengiriman tidak boleh kosong")
    private String shippingAddress;
}
