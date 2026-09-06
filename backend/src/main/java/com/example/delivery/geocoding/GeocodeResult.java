package com.example.delivery.geocoding;

import java.time.Instant;

public record GeocodeResult(Coordinates coordinates, String displayName, Instant fetchedAt, Source source) {
    public enum Source { CACHE, NOMINATIM, UNAVAILABLE, NOT_FOUND }
    public static GeocodeResult unknown(Source source) { return new GeocodeResult(null, null, null, source); }
    public static GeocodeResult from(GeocodeCache entry, Source source) {
        return new GeocodeResult(entry.coordinates(), entry.displayName(), entry.fetchedAt(), source);
    }
}
