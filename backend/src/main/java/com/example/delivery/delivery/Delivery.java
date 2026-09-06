package com.example.delivery.delivery;

import com.example.delivery.geocoding.AddressNormalizer;
import com.example.delivery.geocoding.Coordinates;
import com.example.delivery.geocoding.GeocodeResult;
import com.example.delivery.geocoding.Haversine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "delivery")
public class Delivery {
    private static final int COORDINATE_SCALE = 6;
    private static final int DISTANCE_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_ref", nullable = false, length = 50)
    private String orderReference;

    @Column(name = "dest_address", nullable = false, length = 300)
    private String destinationAddress;

    @Column(name = "dest_lat", precision = 9, scale = 6)
    private BigDecimal destinationLatitude;

    @Column(name = "dest_lng", precision = 9, scale = 6)
    private BigDecimal destinationLongitude;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Zone zone = Zone.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.PLANNED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "geocoded_at")
    private Instant geocodedAt;

    @Column(name = "resolved_address", length = 500)
    private String resolvedAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "geocode_source", nullable = false, length = 20)
    private GeocodeResult.Source geocodeSource = GeocodeResult.Source.UNAVAILABLE;

    protected Delivery() {}

    public static Delivery create(
            String orderReference, String destinationAddress, DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.updateDetails(orderReference, destinationAddress, status);
        return delivery;
    }

    public void updateDetails(
            String orderReference, String destinationAddress, DeliveryStatus status) {
        this.orderReference = orderReference.strip();
        this.destinationAddress = destinationAddress.strip();
        if (status != null) {
            this.status = status;
        }
    }

    public boolean hasDestinationChanged(String newAddress) {
        String currentAddressKey = AddressNormalizer.normalize(destinationAddress);
        String newAddressKey = AddressNormalizer.normalize(newAddress);
        return !currentAddressKey.equals(newAddressKey);
    }

    public void updateDestination(GeocodeResult geocodeResult, Coordinates factoryCoordinates) {
        geocodeSource = geocodeResult.source();
        geocodedAt = geocodeResult.fetchedAt();
        resolvedAddress = geocodeResult.displayName();

        Coordinates destinationCoordinates = geocodeResult.coordinates();
        if (destinationCoordinates == null) {
            clearDestinationLocation();
            return;
        }

        double calculatedDistanceKm =
                Haversine.calculateDistanceKm(factoryCoordinates, destinationCoordinates);
        // Classify before rounding so 49.999 stays LOCAL and 300.001 stays LONG_HAUL.
        zone = Zone.fromDistanceKm(calculatedDistanceKm);
        distanceKm =
                BigDecimal.valueOf(calculatedDistanceKm)
                        .setScale(DISTANCE_SCALE, RoundingMode.HALF_UP);
        destinationLatitude =
                BigDecimal.valueOf(destinationCoordinates.latitude())
                        .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        destinationLongitude =
                BigDecimal.valueOf(destinationCoordinates.longitude())
                        .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private void clearDestinationLocation() {
        destinationLatitude = null;
        destinationLongitude = null;
        distanceKm = null;
        zone = Zone.UNKNOWN;
    }

    public Long getId() {
        return id;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public BigDecimal getDestinationLatitude() {
        return destinationLatitude;
    }

    public BigDecimal getDestinationLongitude() {
        return destinationLongitude;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public Zone getZone() {
        return zone;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getGeocodedAt() {
        return geocodedAt;
    }

    public String getResolvedAddress() {
        return resolvedAddress;
    }

    public GeocodeResult.Source getGeocodeSource() {
        return geocodeSource;
    }
}
