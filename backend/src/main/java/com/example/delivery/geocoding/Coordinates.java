package com.example.delivery.geocoding;

public record Coordinates(double latitude, double longitude) {
    public Coordinates {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }
}
