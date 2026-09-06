package com.example.delivery.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryRequest(
        @NotBlank(message = "Referensi pesanan wajib diisi")
                @Size(max = 50, message = "Referensi maksimal 50 karakter")
                String orderRef,
        @NotBlank(message = "Alamat tujuan wajib diisi")
                @Size(max = 300, message = "Alamat maksimal 300 karakter")
                String destAddress,
        DeliveryStatus status) {}
