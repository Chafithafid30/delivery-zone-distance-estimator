package com.example.delivery.geocoding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "geocode_cache")
public class GeocodeCache {
    private static final int COORDINATE_SCALE = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address", nullable = false, unique = true, length = 300)
    private String normalizedAddress;

    @Column(name = "lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "display_name", length = 500)
    private String displayName;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected GeocodeCache() {}

    public GeocodeCache(
            String normalizedAddress, GeocodingProvider.Place resolvedPlace, Instant fetchedAt) {
        this.normalizedAddress = normalizedAddress;
        this.latitude =
                BigDecimal.valueOf(resolvedPlace.coordinates().latitude())
                        .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        this.longitude =
                BigDecimal.valueOf(resolvedPlace.coordinates().longitude())
                        .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        this.displayName = resolvedPlace.displayName();
        this.fetchedAt = fetchedAt;
    }

    public Coordinates coordinates() {
        return new Coordinates(latitude.doubleValue(), longitude.doubleValue());
    }

    public String displayName() {
        return displayName;
    }

    public Instant fetchedAt() {
        return fetchedAt;
    }
}
