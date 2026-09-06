package com.example.delivery.config;

import com.example.delivery.geocoding.Coordinates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FactoryLocation {
    private final Coordinates coordinates;

    public FactoryLocation(
            @Value("${app.factory.latitude}") double latitude,
            @Value("${app.factory.longitude}") double longitude) {
        this.coordinates = new Coordinates(latitude, longitude);
    }

    public Coordinates coordinates() {
        return coordinates;
    }
}
