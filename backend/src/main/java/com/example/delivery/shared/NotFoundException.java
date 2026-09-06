package com.example.delivery.shared;

public class NotFoundException extends RuntimeException {
    public NotFoundException(Long id) { super("Pengiriman dengan ID " + id + " tidak ditemukan"); }
}
