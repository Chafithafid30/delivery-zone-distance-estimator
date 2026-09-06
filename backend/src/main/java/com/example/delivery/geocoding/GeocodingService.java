package com.example.delivery.geocoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.geocoding.mode", havingValue = "local", matchIfMissing = true)
public class GeocodingService implements AddressResolver {
    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

    private final GeocodeCacheRepository cacheRepository;
    private final GeocodingProvider geocodingProvider;
    private final GeocodingRequestPolicy requestPolicy;

    public GeocodingService(
            GeocodeCacheRepository cacheRepository,
            GeocodingProvider geocodingProvider,
            GeocodingRequestPolicy requestPolicy) {
        this.cacheRepository = cacheRepository;
        this.geocodingProvider = geocodingProvider;
        this.requestPolicy = requestPolicy;
    }

    @Override
    public GeocodeResult resolveAddress(String address) {
        String normalizedAddress = AddressNormalizer.normalize(address);
        Optional<GeocodeResult> cachedResult = findCachedResult(normalizedAddress);

        // Cached addresses remain available even while another lookup is waiting on the API.
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }
        return resolveUncachedAddress(normalizedAddress);
    }

    private synchronized GeocodeResult resolveUncachedAddress(String normalizedAddress) {
        // A previous request may have filled the cache while this request waited for the lock.
        Optional<GeocodeResult> cachedResult = findCachedResult(normalizedAddress);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }
        if (requestPolicy.isInCooldown()) {
            return GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE);
        }

        try {
            requestPolicy.awaitNextRequest();
            return fetchAndCacheAddress(normalizedAddress);
        } catch (ProviderUnavailableException exception) {
            requestPolicy.recordFailure();
            // Do not include customer addresses or provider response bodies in logs.
            logger.warn(
                    "Geocoding provider unavailable; unresolved deliveries will use UNKNOWN during"
                            + " cooldown");
            return findCachedResult(normalizedAddress)
                    .orElseGet(() -> GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE);
        }
    }

    private GeocodeResult fetchAndCacheAddress(String normalizedAddress) {
        Optional<GeocodingProvider.Place> resolvedPlace;
        try {
            resolvedPlace = geocodingProvider.lookupAddress(normalizedAddress);
            requestPolicy.recordSuccess();
        } finally {
            requestPolicy.recordRequestCompleted();
        }

        if (resolvedPlace.isEmpty()) {
            return GeocodeResult.unknown(GeocodeResult.Source.NOT_FOUND);
        }
        GeocodeCache cacheEntry =
                new GeocodeCache(normalizedAddress, resolvedPlace.get(), Instant.now());
        GeocodeCache savedEntry = cacheRepository.saveAndFlush(cacheEntry);
        return GeocodeResult.from(savedEntry, GeocodeResult.Source.NOMINATIM);
    }

    private Optional<GeocodeResult> findCachedResult(String normalizedAddress) {
        return cacheRepository
                .findByNormalizedAddress(normalizedAddress)
                .map(cacheEntry -> GeocodeResult.from(cacheEntry, GeocodeResult.Source.CACHE));
    }
}
