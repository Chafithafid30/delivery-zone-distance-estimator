package com.example.delivery.config;

import com.example.delivery.geocoding.Coordinates;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@Profile("!geocoder")
public class ConfigController {
    private final FactoryLocation factoryLocation;

    public ConfigController(FactoryLocation factoryLocation) {
        this.factoryLocation = factoryLocation;
    }

    @GetMapping
    public ConfigResponse getConfiguration() {
        return new ConfigResponse(factoryLocation.coordinates());
    }

    public record ConfigResponse(Coordinates factory) {}
}
