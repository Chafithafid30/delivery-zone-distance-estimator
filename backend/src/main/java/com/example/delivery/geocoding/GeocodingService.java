package com.example.delivery.geocoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class GeocodingService {
    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private final GeocodeCacheRepository cache;
    private final GeocodingProvider provider;
    private final long intervalNanos;
    private final long cooldownNanos;
    private long lastCallFinished;
    private long failureAt;
    private boolean hasCalled;
    private boolean coolingDown;

    public GeocodingService(GeocodeCacheRepository cache, GeocodingProvider provider,
            @Value("${app.geocoding.interval-ms}") long intervalMs,
            @Value("${app.geocoding.cooldown-seconds}") long cooldownSeconds) {
        this.cache = cache;
        this.provider = provider;
        this.intervalNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(1000, intervalMs));
        this.cooldownNanos = TimeUnit.SECONDS.toNanos(Math.max(0, cooldownSeconds));
    }

    public GeocodeResult resolve(String address) {
        String key = AddressNormalizer.normalize(address);
        var existing = cache.findByAddress(key);
        // Cache hits do not wait for the outbound request lock or an unhealthy provider.
        if (existing.isPresent()) return GeocodeResult.from(existing.get(), GeocodeResult.Source.CACHE);
        return resolveMiss(key);
    }

    private synchronized GeocodeResult resolveMiss(String key) {
        // Recheck under lock: concurrent requests for the same address call the API once.
        var existing = cache.findByAddress(key);
        if (existing.isPresent()) return GeocodeResult.from(existing.get(), GeocodeResult.Source.CACHE);
        if (coolingDown && System.nanoTime() - failureAt < cooldownNanos) {
            return GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE);
        }
        try {
            if (hasCalled) {
                long remaining = intervalNanos - (System.nanoTime() - lastCallFinished);
                if (remaining > 0) TimeUnit.NANOSECONDS.sleep(remaining);
            }
            var place = provider.lookup(key);
            coolingDown = false;
            if (place.isEmpty()) return GeocodeResult.unknown(GeocodeResult.Source.NOT_FOUND);
            var entry = cache.saveAndFlush(new GeocodeCache(key, place.get(), Instant.now()));
            return GeocodeResult.from(entry, GeocodeResult.Source.NOMINATIM);
        } catch (ProviderUnavailableException e) {
            failureAt = System.nanoTime();
            coolingDown = true;
            // Addresses and response payloads are deliberately omitted from logs.
            log.warn("Geocoding provider unavailable; serving unresolved deliveries during cooldown");
            return cache.findByAddress(key)
                    .map(entry -> GeocodeResult.from(entry, GeocodeResult.Source.CACHE))
                    .orElseGet(() -> GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE);
        } finally {
            lastCallFinished = System.nanoTime();
            hasCalled = true;
        }
    }
}
