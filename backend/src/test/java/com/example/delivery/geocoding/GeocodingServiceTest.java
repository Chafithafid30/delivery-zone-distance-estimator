package com.example.delivery.geocoding;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

class GeocodingServiceTest {
    private final GeocodeCacheRepository cache = mock(GeocodeCacheRepository.class);
    private final GeocodingProvider provider = mock(GeocodingProvider.class);
    private final GeocodingService service =
            new GeocodingService(cache, provider, new GeocodingRequestPolicy(1100, 30));
    private final GeocodingProvider.Place place =
            new GeocodingProvider.Place(new Coordinates(-6.1751, 106.8650), "Monas");

    @Test
    void cachedAddressDoesNotCallProviderEvenWhenItIsDown() {
        GeocodeCache entry =
                new GeocodeCache("monas jakarta", place, Instant.parse("2026-01-01T00:00:00Z"));
        when(cache.findByNormalizedAddress("monas jakarta")).thenReturn(Optional.of(entry));
        GeocodeResult result = service.resolveAddress("  MONAS   Jakarta  ");
        assertThat(result.source()).isEqualTo(GeocodeResult.Source.CACHE);
        assertThat(result.fetchedAt()).isEqualTo(entry.fetchedAt());
        verifyNoInteractions(provider);
    }

    @Test
    void missSavesResultAndSecondRequestUsesCache() {
        AtomicReference<GeocodeCache> stored = new AtomicReference<>();
        when(cache.findByNormalizedAddress(anyString()))
                .thenAnswer(call -> Optional.ofNullable(stored.get()));
        when(provider.lookupAddress("monas")).thenReturn(Optional.of(place));
        when(cache.saveAndFlush(any()))
                .thenAnswer(
                        call -> {
                            stored.set(call.getArgument(0));
                            return stored.get();
                        });
        assertThat(service.resolveAddress("Monas").source())
                .isEqualTo(GeocodeResult.Source.NOMINATIM);
        assertThat(service.resolveAddress("MONAS").source()).isEqualTo(GeocodeResult.Source.CACHE);
        verify(provider, times(1)).lookupAddress("monas");
    }

    @Test
    void outageReturnsUnknownAndCooldownSkipsFurtherRequests() {
        when(cache.findByNormalizedAddress(anyString())).thenReturn(Optional.empty());
        when(provider.lookupAddress(anyString()))
                .thenThrow(new ProviderUnavailableException("HTTP 503"));
        assertThat(service.resolveAddress("Bandung").coordinates()).isNull();
        assertThat(service.resolveAddress("Surabaya").source())
                .isEqualTo(GeocodeResult.Source.UNAVAILABLE);
        verify(provider, times(1)).lookupAddress(anyString());
        verify(cache, never()).saveAndFlush(any());
    }

    @Test
    void emptyProviderResultIsNotCachedAsFakeCoordinates() {
        when(cache.findByNormalizedAddress(anyString())).thenReturn(Optional.empty());
        when(provider.lookupAddress(anyString())).thenReturn(Optional.empty());
        assertThat(service.resolveAddress("Unresolvable").source())
                .isEqualTo(GeocodeResult.Source.NOT_FOUND);
        verify(cache, never()).saveAndFlush(any());
    }

    @Test
    void providerFailureRechecksCache() {
        GeocodeCache entry = new GeocodeCache("monas", place, Instant.now());
        when(cache.findByNormalizedAddress("monas"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(entry));
        when(provider.lookupAddress("monas"))
                .thenThrow(new ProviderUnavailableException("HTTP 503"));
        assertThat(service.resolveAddress("Monas").source()).isEqualTo(GeocodeResult.Source.CACHE);
    }

    @Test
    void concurrentIdenticalAddressesCallProviderOnlyOnce() throws Exception {
        AtomicReference<GeocodeCache> stored = new AtomicReference<>();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(cache.findByNormalizedAddress(anyString()))
                .thenAnswer(call -> Optional.ofNullable(stored.get()));
        when(provider.lookupAddress("monas"))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            if (!release.await(3, TimeUnit.SECONDS))
                                throw new AssertionError("Test timed out");
                            return Optional.of(place);
                        });
        when(cache.saveAndFlush(any()))
                .thenAnswer(
                        call -> {
                            stored.set(call.getArgument(0));
                            return stored.get();
                        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<GeocodeResult> first = executor.submit(() -> service.resolveAddress("Monas"));
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            Future<GeocodeResult> second = executor.submit(() -> service.resolveAddress(" MONAS "));
            release.countDown();
            assertThat(first.get(3, TimeUnit.SECONDS).source())
                    .isEqualTo(GeocodeResult.Source.NOMINATIM);
            assertThat(second.get(3, TimeUnit.SECONDS).source())
                    .isEqualTo(GeocodeResult.Source.CACHE);
            verify(provider, times(1)).lookupAddress("monas");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void distinctCacheMissesRespectGlobalRateLimit() {
        when(cache.findByNormalizedAddress(anyString())).thenReturn(Optional.empty());
        when(provider.lookupAddress(anyString())).thenReturn(Optional.empty());
        service.resolveAddress("first");
        long started = System.nanoTime();
        service.resolveAddress("second");
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
                .isGreaterThanOrEqualTo(1000);
    }
}
