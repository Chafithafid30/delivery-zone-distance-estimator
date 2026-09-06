package com.example.delivery.delivery;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(Long deliveryId) {
        super("Pengiriman dengan ID " + deliveryId + " tidak ditemukan");
    }
}
