package com.example.delivery.config;

import com.example.delivery.geocoding.Coordinates;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final FactoryLocation factory;
    public ConfigController(FactoryLocation factory) { this.factory = factory; }
    @GetMapping
    public ConfigResponse config() { return new ConfigResponse(factory.coordinates()); }
    public record ConfigResponse(Coordinates factory) {}
}
