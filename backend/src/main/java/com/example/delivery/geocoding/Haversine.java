package com.example.delivery.geocoding;

public final class Haversine {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private Haversine() {}

    public static double calculateDistanceKm(Coordinates origin, Coordinates destination) {
        double originLatitudeRadians = Math.toRadians(origin.latitude());
        double destinationLatitudeRadians = Math.toRadians(destination.latitude());
        double latitudeDeltaRadians = destinationLatitudeRadians - originLatitudeRadians;
        double longitudeDeltaRadians = Math.toRadians(destination.longitude() - origin.longitude());

        double haversineTerm =
                Math.pow(Math.sin(latitudeDeltaRadians / 2), 2)
                        + Math.cos(originLatitudeRadians)
                                * Math.cos(destinationLatitudeRadians)
                                * Math.pow(Math.sin(longitudeDeltaRadians / 2), 2);

        // Round-off at antipodal points can otherwise make the square root return NaN.
        haversineTerm = Math.max(0, Math.min(1, haversineTerm));
        double angularDistanceRadians =
                2 * Math.atan2(Math.sqrt(haversineTerm), Math.sqrt(1 - haversineTerm));
        return EARTH_RADIUS_KM * angularDistanceRadians;
    }
}
