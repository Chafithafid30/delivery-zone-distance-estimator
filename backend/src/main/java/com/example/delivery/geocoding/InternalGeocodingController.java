package com.example.delivery.geocoding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("geocoder")
@RequestMapping("/internal/geocoding")
public class InternalGeocodingController {
    private final AddressResolver addressResolver;

    public InternalGeocodingController(AddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    @PostMapping
    public GeocodeResult resolveAddress(@Valid @RequestBody GeocodingRequest request) {
        return addressResolver.resolveAddress(request.address());
    }

    public record GeocodingRequest(@NotBlank @Size(max = 300) String address) {}
}
