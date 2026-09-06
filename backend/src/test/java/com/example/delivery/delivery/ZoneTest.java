package com.example.delivery.delivery;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ZoneTest {
    @ParameterizedTest
    @CsvSource({
        "0,LOCAL",
        "49.999,LOCAL",
        "50,REGIONAL",
        "50.001,REGIONAL",
        "299.999,REGIONAL",
        "300,REGIONAL",
        "300.001,LONG_HAUL"
    })
    void exactBoundaries(double km, Zone expected) {
        assertThat(Zone.fromDistanceKm(km)).isEqualTo(expected);
    }

    @Test
    void rejectsNonFiniteOrNegativeDistance() {
        assertThatThrownBy(() -> Zone.fromDistanceKm(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Zone.fromDistanceKm(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
