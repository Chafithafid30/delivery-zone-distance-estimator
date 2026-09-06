package com.example.delivery.delivery;

import com.example.delivery.geocoding.GeocodeResult;

import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryResponse(
        Long id,
        String orderRef,
        String destAddress,
        BigDecimal destLat,
        BigDecimal destLng,
        BigDecimal distanceKm,
        Zone zone,
        DeliveryStatus status,
        Instant createdAt,
        Instant geocodedAt,
        String resolvedAddress,
        GeocodeResult.Source geocodeSource,
        ShippingEstimate estimate) {

    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrderReference(),
                delivery.getDestinationAddress(),
                delivery.getDestinationLatitude(),
                delivery.getDestinationLongitude(),
                delivery.getDistanceKm(),
                delivery.getZone(),
                delivery.getStatus(),
                delivery.getCreatedAt(),
                delivery.getGeocodedAt(),
                delivery.getResolvedAddress(),
                delivery.getGeocodeSource(),
                ShippingEstimate.forZone(delivery.getZone()));
    }
}
