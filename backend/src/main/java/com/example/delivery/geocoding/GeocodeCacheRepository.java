package com.example.delivery.geocoding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeocodeCacheRepository extends JpaRepository<GeocodeCache, Long> {
    Optional<GeocodeCache> findByNormalizedAddress(String normalizedAddress);
}
