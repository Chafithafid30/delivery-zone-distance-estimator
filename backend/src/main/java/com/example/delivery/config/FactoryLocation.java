package com.example.delivery.config;

import com.example.delivery.geocoding.Coordinates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FactoryLocation {
    private final Coordinates coordinates;
    public FactoryLocation(@Value("${app.factory.latitude}") double lat,
                           @Value("${app.factory.longitude}") double lng) {
        this.coordinates = new Coordinates(lat, lng);
    }
    public Coordinates coordinates() { return coordinates; }
}
