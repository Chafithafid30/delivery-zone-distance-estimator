package com.example.delivery.geocoding;

import java.util.Optional;

/** The external HTTP boundary. Replace this adapter to use another provider. */
public interface GeocodingProvider {
    Optional<Place> lookup(String address) throws ProviderUnavailableException;

    record Place(Coordinates coordinates, String displayName) {}
}
