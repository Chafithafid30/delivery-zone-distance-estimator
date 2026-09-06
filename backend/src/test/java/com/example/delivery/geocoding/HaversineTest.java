package com.example.delivery.geocoding;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HaversineTest {
    @Test
    void samePointIsZero() {
        Coordinates jakarta = new Coordinates(-6.1751, 106.8650);
        assertThat(Haversine.calculateDistanceKm(jakarta, jakarta)).isZero();
    }

    @Test
    void quarterEquatorMatchesKnownArcLength() {
        assertThat(Haversine.calculateDistanceKm(new Coordinates(0, 0), new Coordinates(0, 90)))
                .isCloseTo(10007.557221, within(0.001));
    }

    @Test
    void antipodalPointsAreFinite() {
        assertThat(Haversine.calculateDistanceKm(new Coordinates(0, 0), new Coordinates(0, 180)))
                .isCloseTo(20015.114442, within(0.001));
    }

    @Test
    void crossesDateLineViaShorterArc() {
        assertThat(Haversine.calculateDistanceKm(new Coordinates(0, 179), new Coordinates(0, -179)))
                .isCloseTo(222.39016, within(0.001));
    }

    @Test
    void distanceIsSymmetric() {
        Coordinates jakarta = new Coordinates(-6.1751, 106.8650);
        Coordinates bandung = new Coordinates(-6.9175, 107.6191);
        assertThat(Haversine.calculateDistanceKm(jakarta, bandung))
                .isCloseTo(Haversine.calculateDistanceKm(bandung, jakarta), within(0.000001));
    }

    @Test
    void invalidCoordinatesAreRejected() {
        assertThatThrownBy(() -> new Coordinates(Double.NaN, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinates(91, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinates(0, -181))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
