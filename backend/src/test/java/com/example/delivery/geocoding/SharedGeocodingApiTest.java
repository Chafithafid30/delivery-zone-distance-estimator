package com.example.delivery.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties =
                "spring.datasource.url=jdbc:h2:mem:shared-geocoder;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "geocoder"})
class SharedGeocodingApiTest {
    @LocalServerPort private int port;
    @Autowired private GeocodeCacheRepository cache;
    @Autowired private ObjectMapper mapper;
    @Autowired private TestRestTemplate http;
    @MockitoBean private GeocodingProvider provider;

    @BeforeEach
    void clearCache() {
        cache.deleteAll();
    }

    @Test
    void independentBackendClientsShareOneLookupAndPersistentCache() throws Exception {
        String serviceUrl = "http://127.0.0.1:" + port;
        RemoteAddressResolver firstClient =
                new RemoteAddressResolver(cache, mapper, serviceUrl, 10);
        RemoteAddressResolver secondClient =
                new RemoteAddressResolver(cache, mapper, serviceUrl, 10);
        CountDownLatch start = new CountDownLatch(1);
        when(provider.lookupAddress("monas jakarta"))
                .thenReturn(
                        Optional.of(
                                new GeocodingProvider.Place(
                                        new Coordinates(-6.1751, 106.865), "Monas")));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<GeocodeResult> first =
                    executor.submit(
                            () -> {
                                start.await();
                                return firstClient.resolveAddress("Monas Jakarta");
                            });
            Future<GeocodeResult> second =
                    executor.submit(
                            () -> {
                                start.await();
                                return secondClient.resolveAddress(" MONAS   JAKARTA ");
                            });
            start.countDown();
            GeocodeResult firstResult = first.get(10, TimeUnit.SECONDS);
            GeocodeResult secondResult = second.get(10, TimeUnit.SECONDS);
            assertThat(firstResult.coordinates()).isEqualTo(new Coordinates(-6.1751, 106.865));
            assertThat(secondResult.coordinates()).isEqualTo(firstResult.coordinates());
            assertThat(java.util.List.of(firstResult.source(), secondResult.source()))
                    .containsExactlyInAnyOrder(
                            GeocodeResult.Source.NOMINATIM, GeocodeResult.Source.CACHE);
            assertThat(cache.count()).isEqualTo(1);
            assertThat(secondClient.resolveAddress("monas jakarta").source())
                    .isEqualTo(GeocodeResult.Source.CACHE);
            verify(provider, times(1)).lookupAddress("monas jakarta");
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void workerValidatesAddressesAndDoesNotExposeDeliveryEndpoints() {
        assertThat(
                        http.postForEntity(
                                        "/internal/geocoding", Map.of("address", " "), String.class)
                                .getStatusCode()
                                .value())
                .isEqualTo(400);
        assertThat(http.getForEntity("/api/deliveries", String.class).getStatusCode().value())
                .isEqualTo(404);
        verifyNoInteractions(provider);
    }
}
