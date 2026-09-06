package com.example.delivery.delivery;

/** Illustrative flat tariffs per shipment, not carrier quotes or road-based ETAs. */
public record ShippingEstimate(long costIdr, int minDays, int maxDays) {
    public static ShippingEstimate forZone(Zone zone) {
        return switch (zone) {
            case LOCAL -> new ShippingEstimate(25_000, 1, 1);
            case REGIONAL -> new ShippingEstimate(75_000, 2, 3);
            case LONG_HAUL -> new ShippingEstimate(150_000, 4, 7);
            case UNKNOWN -> null;
        };
    }
}
