package com.example.delivery.delivery;

public enum Zone {
    LOCAL,
    REGIONAL,
    LONG_HAUL,
    UNKNOWN;

    private static final double LOCAL_DISTANCE_LIMIT_KM = 50;
    private static final double REGIONAL_MAX_DISTANCE_KM = 300;

    public static Zone fromDistanceKm(double distanceKm) {
        if (!Double.isFinite(distanceKm) || distanceKm < 0) {
            throw new IllegalArgumentException("Invalid distance");
        }
        if (distanceKm < LOCAL_DISTANCE_LIMIT_KM) {
            return LOCAL;
        }
        if (distanceKm <= REGIONAL_MAX_DISTANCE_KM) {
            return REGIONAL;
        }
        return LONG_HAUL;
    }
}
