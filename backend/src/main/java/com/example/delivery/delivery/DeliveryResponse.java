package com.example.delivery.delivery;

import com.example.delivery.geocoding.GeocodeResult;
import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryResponse(Long id, String orderRef, String destAddress,
        BigDecimal destLat, BigDecimal destLng, BigDecimal distanceKm,
        Zone zone, DeliveryStatus status, Instant createdAt, Instant geocodedAt,
        String resolvedAddress, GeocodeResult.Source geocodeSource, ShippingEstimate estimate) {
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(d.id, d.orderRef, d.destAddress, d.destLat, d.destLng,
                d.distanceKm, d.zone, d.status, d.createdAt, d.geocodedAt,
                d.resolvedAddress, d.geocodeSource, ShippingEstimate.forZone(d.zone));
    }
}
