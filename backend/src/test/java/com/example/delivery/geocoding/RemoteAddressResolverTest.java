package com.example.delivery.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class RemoteAddressResolverTest {
    private final GeocodeCacheRepository cache = mock(GeocodeCacheRepository.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private RemoteAddressResolver resolver;
    private final GeocodeCache entry =
            new GeocodeCache(
                    "monas jakarta",
                    new GeocodingProvider.Place(new Coordinates(-6.1751, 106.865), "Monas"),
                    Instant.parse("2026-01-01T00:00:00Z"));

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        resolver =
                new RemoteAddressResolver(
                        cache, mapper, "http://127.0.0.1:" + server.getAddress().getPort(), 1);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(int status, String body) {
        server.createContext(
                "/internal/geocoding",
                exchange -> {
                    requests.incrementAndGet();
                    requestBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(bytes);
                    }
                });
    }

    @Test
    void cachedAddressSkipsTheWorker() {
        when(cache.findByNormalizedAddress("monas jakarta")).thenReturn(Optional.of(entry));
        respond(503, "unavailable");
        GeocodeResult result = resolver.resolveAddress(" MONAS   Jakarta ");
        assertThat(result.source()).isEqualTo(GeocodeResult.Source.CACHE);
        assertThat(result.fetchedAt()).isEqualTo(entry.fetchedAt());
        assertThat(requests.get()).isZero();
    }

    @Test
    void sendsNormalizedAddressAndPreservesWorkerMetadata() throws Exception {
        GeocodeResult expected = GeocodeResult.from(entry, GeocodeResult.Source.NOMINATIM);
        respond(200, mapper.writeValueAsString(expected));
        assertThat(resolver.resolveAddress(" MONAS   Jakarta ")).isEqualTo(expected);
        assertThat(mapper.readTree(requestBody.get()).get("address").asText())
                .isEqualTo("monas jakarta");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "not json",
                "null",
                "{}",
                "{\"source\":\"NOMINATIM\"}",
                "{\"coordinates\":{\"latitude\":91,\"longitude\":0},\"source\":\"NOMINATIM\",\"fetchedAt\":\"2026-01-01T00:00:00Z\"}"
            })
    void invalidWorkerResponseBecomesUnavailable(String body) {
        respond(200, body);
        assertThat(resolver.resolveAddress("monas jakarta"))
                .isEqualTo(GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE));
    }

    @Test
    void workerFailureRechecksCache() {
        when(cache.findByNormalizedAddress("monas jakarta"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(entry));
        respond(503, "unavailable");
        assertThat(resolver.resolveAddress("monas jakarta").source())
                .isEqualTo(GeocodeResult.Source.CACHE);
    }

    @Test
    void unavailableWorkerReturnsUnknownWithoutPublicApiFallback() {
        server.stop(0);
        assertThat(resolver.resolveAddress("uncached address"))
                .isEqualTo(GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE));
    }
}
