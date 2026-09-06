package com.example.delivery.geocoding;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "geocode_cache")
public class GeocodeCache {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 300)
    private String address;
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;
    @Column(name = "display_name", length = 500)
    private String displayName;
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected GeocodeCache() {}

    public GeocodeCache(String address, GeocodingProvider.Place place, Instant fetchedAt) {
        this.address = address;
        this.lat = BigDecimal.valueOf(place.coordinates().latitude()).setScale(6, RoundingMode.HALF_UP);
        this.lng = BigDecimal.valueOf(place.coordinates().longitude()).setScale(6, RoundingMode.HALF_UP);
        this.displayName = place.displayName();
        this.fetchedAt = fetchedAt;
    }

    public Coordinates coordinates() { return new Coordinates(lat.doubleValue(), lng.doubleValue()); }
    public String displayName() { return displayName; }
    public Instant fetchedAt() { return fetchedAt; }
}
