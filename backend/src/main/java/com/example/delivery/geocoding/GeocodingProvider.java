package com.example.delivery.geocoding;

import java.util.Optional;

/** The external HTTP boundary. Replace this adapter to use another provider. */
public interface GeocodingProvider {
    /** Empty means no matching address; unavailable or invalid responses must throw. */
    Optional<Place> lookupAddress(String address) throws ProviderUnavailableException;

    record Place(Coordinates coordinates, String displayName) {}
}
