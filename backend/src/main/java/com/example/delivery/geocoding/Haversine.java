package com.example.delivery.geocoding;

public final class Haversine {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private Haversine() {}

    public static double distanceKm(Coordinates origin, Coordinates destination) {
        double lat1 = Math.toRadians(origin.latitude());
        double lat2 = Math.toRadians(destination.latitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(destination.longitude() - origin.longitude());
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        // Floating point round-off can otherwise make antipodal points return NaN.
        a = Math.max(0, Math.min(1, a));
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
