package com.example.delivery.geocoding;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HaversineTest {
    @Test void samePointIsZero() {
        Coordinates jakarta = new Coordinates(-6.1751, 106.8650);
        assertThat(Haversine.distanceKm(jakarta, jakarta)).isZero();
    }

    @Test void quarterEquatorMatchesKnownArcLength() {
        assertThat(Haversine.distanceKm(new Coordinates(0, 0), new Coordinates(0, 90)))
                .isCloseTo(10007.557221, within(0.001));
    }

    @Test void antipodalPointsAreFinite() {
        assertThat(Haversine.distanceKm(new Coordinates(0, 0), new Coordinates(0, 180)))
                .isCloseTo(20015.114442, within(0.001));
    }

    @Test void crossesDateLineViaShorterArc() {
        assertThat(Haversine.distanceKm(new Coordinates(0, 179), new Coordinates(0, -179)))
                .isCloseTo(222.39016, within(0.001));
    }

    @Test void distanceIsSymmetric() {
        Coordinates jakarta = new Coordinates(-6.1751, 106.8650);
        Coordinates bandung = new Coordinates(-6.9175, 107.6191);
        assertThat(Haversine.distanceKm(jakarta, bandung))
                .isCloseTo(Haversine.distanceKm(bandung, jakarta), within(0.000001));
    }

    @Test void invalidCoordinatesAreRejected() {
        assertThatThrownBy(() -> new Coordinates(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinates(91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinates(0, -181)).isInstanceOf(IllegalArgumentException.class);
    }
}
