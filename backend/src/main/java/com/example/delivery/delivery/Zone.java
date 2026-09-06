package com.example.delivery.delivery;

public enum Zone {
    LOCAL, REGIONAL, LONG_HAUL, UNKNOWN;

    public static Zone fromDistance(double km) {
        if (!Double.isFinite(km) || km < 0) throw new IllegalArgumentException("Invalid distance");
        if (km < 50) return LOCAL;
        if (km <= 300) return REGIONAL;
        return LONG_HAUL;
    }
}
