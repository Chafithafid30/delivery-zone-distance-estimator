package com.example.delivery.geocoding;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeocodingServiceTest {
    private final GeocodeCacheRepository cache = mock(GeocodeCacheRepository.class);
    private final GeocodingProvider provider = mock(GeocodingProvider.class);
    private final GeocodingService service = new GeocodingService(cache, provider, 1100, 30);
    private final GeocodingProvider.Place place = new GeocodingProvider.Place(new Coordinates(-6.1751, 106.8650), "Monas");

    @Test void cachedAddressDoesNotCallProviderEvenWhenItIsDown() {
        GeocodeCache entry = new GeocodeCache("monas jakarta", place, Instant.parse("2026-01-01T00:00:00Z"));
        when(cache.findByAddress("monas jakarta")).thenReturn(Optional.of(entry));
        GeocodeResult result = service.resolve("  MONAS   Jakarta  ");
        assertThat(result.source()).isEqualTo(GeocodeResult.Source.CACHE);
        assertThat(result.fetchedAt()).isEqualTo(entry.fetchedAt());
        verifyNoInteractions(provider);
    }

    @Test void missSavesResultAndSecondRequestUsesCache() {
        AtomicReference<GeocodeCache> stored = new AtomicReference<>();
        when(cache.findByAddress(anyString())).thenAnswer(call -> Optional.ofNullable(stored.get()));
        when(provider.lookup("monas")).thenReturn(Optional.of(place));
        when(cache.saveAndFlush(any())).thenAnswer(call -> { stored.set(call.getArgument(0)); return stored.get(); });
        assertThat(service.resolve("Monas").source()).isEqualTo(GeocodeResult.Source.NOMINATIM);
        assertThat(service.resolve("MONAS").source()).isEqualTo(GeocodeResult.Source.CACHE);
        verify(provider, times(1)).lookup("monas");
    }

    @Test void outageReturnsUnknownAndCooldownSkipsFurtherRequests() {
        when(cache.findByAddress(anyString())).thenReturn(Optional.empty());
        when(provider.lookup(anyString())).thenThrow(new ProviderUnavailableException("HTTP 503"));
        assertThat(service.resolve("Bandung").coordinates()).isNull();
        assertThat(service.resolve("Surabaya").source()).isEqualTo(GeocodeResult.Source.UNAVAILABLE);
        verify(provider, times(1)).lookup(anyString());
        verify(cache, never()).saveAndFlush(any());
    }

    @Test void emptyProviderResultIsNotCachedAsFakeCoordinates() {
        when(cache.findByAddress(anyString())).thenReturn(Optional.empty());
        when(provider.lookup(anyString())).thenReturn(Optional.empty());
        assertThat(service.resolve("Unresolvable").source()).isEqualTo(GeocodeResult.Source.NOT_FOUND);
        verify(cache, never()).saveAndFlush(any());
    }

    @Test void providerFailureRechecksCache() {
        GeocodeCache entry = new GeocodeCache("monas", place, Instant.now());
        when(cache.findByAddress("monas")).thenReturn(Optional.empty())
                .thenReturn(Optional.empty()).thenReturn(Optional.of(entry));
        when(provider.lookup("monas")).thenThrow(new ProviderUnavailableException("HTTP 503"));
        assertThat(service.resolve("Monas").source()).isEqualTo(GeocodeResult.Source.CACHE);
    }

    @Test void concurrentIdenticalAddressesCallProviderOnlyOnce() throws Exception {
        AtomicReference<GeocodeCache> stored = new AtomicReference<>();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(cache.findByAddress(anyString())).thenAnswer(call -> Optional.ofNullable(stored.get()));
        when(provider.lookup("monas")).thenAnswer(call -> {
            entered.countDown();
            if (!release.await(3, TimeUnit.SECONDS)) throw new AssertionError("Test timed out");
            return Optional.of(place);
        });
        when(cache.saveAndFlush(any())).thenAnswer(call -> { stored.set(call.getArgument(0)); return stored.get(); });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<GeocodeResult> first = executor.submit(() -> service.resolve("Monas"));
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
            Future<GeocodeResult> second = executor.submit(() -> service.resolve(" MONAS "));
            release.countDown();
            assertThat(first.get(3, TimeUnit.SECONDS).source()).isEqualTo(GeocodeResult.Source.NOMINATIM);
            assertThat(second.get(3, TimeUnit.SECONDS).source()).isEqualTo(GeocodeResult.Source.CACHE);
            verify(provider, times(1)).lookup("monas");
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void distinctCacheMissesRespectGlobalRateLimit() {
        when(cache.findByAddress(anyString())).thenReturn(Optional.empty());
        when(provider.lookup(anyString())).thenReturn(Optional.empty());
        service.resolve("first");
        long started = System.nanoTime();
        service.resolve("second");
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isGreaterThanOrEqualTo(1000);
    }
}
