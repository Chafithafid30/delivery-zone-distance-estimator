package com.example.delivery.delivery;

import com.example.delivery.geocoding.GeocodeResult;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "delivery")
public class Delivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "order_ref", nullable = false, length = 50)
    String orderRef;
    @Column(name = "dest_address", nullable = false, length = 300)
    String destAddress;
    @Column(name = "dest_lat", precision = 9, scale = 6)
    BigDecimal destLat;
    @Column(name = "dest_lng", precision = 9, scale = 6)
    BigDecimal destLng;
    @Column(name = "distance_km", precision = 10, scale = 2)
    BigDecimal distanceKm;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    Zone zone = Zone.UNKNOWN;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    DeliveryStatus status = DeliveryStatus.PLANNED;
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt = Instant.now();
    @Column(name = "geocoded_at")
    Instant geocodedAt;
    @Column(name = "resolved_address", length = 500)
    String resolvedAddress;
    @Enumerated(EnumType.STRING) @Column(name = "geocode_source", nullable = false, length = 20)
    GeocodeResult.Source geocodeSource = GeocodeResult.Source.UNAVAILABLE;

    protected Delivery() {}
}
