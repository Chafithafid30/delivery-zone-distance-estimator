package com.example.delivery.geocoding;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

class NominatimClientTest {
    private HttpServer server;
    private NominatimClient client;
    private final AtomicReference<String> userAgent = new AtomicReference<>();
    private final AtomicReference<String> query = new AtomicReference<>();

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client =
                new NominatimClient(
                        new ObjectMapper(),
                        "http://127.0.0.1:" + server.getAddress().getPort(),
                        "DeliveryZone-Test/1.0",
                        1);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void respond(int status, String body) {
        server.createContext(
                "/search",
                exchange -> {
                    userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
                    query.set(exchange.getRequestURI().getRawQuery());
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                });
    }

    @Test
    void parsesResponseSendsDescriptiveHeaderAndEncodesAddress() {
        respond(
                200,
                "[{\"lat\":\"-6.1751\",\"lon\":\"106.8650\",\"display_name\":\"Monas, Jakarta\"}]");
        var place = client.lookupAddress("Monas & Jakarta").orElseThrow();
        assertThat(place.coordinates().latitude()).isEqualTo(-6.1751);
        assertThat(userAgent.get()).isEqualTo("DeliveryZone-Test/1.0");
        assertThat(query.get()).contains("q=Monas+%26+Jakarta", "format=json", "limit=1");
    }

    @Test
    void noResultsAreAnEmptyOptional() {
        respond(200, "[]");
        assertThat(client.lookupAddress("unknown")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 429, 500, 503})
    void httpErrorsBecomeUnavailable(int status) {
        respond(status, "unavailable");
        assertThatThrownBy(() -> client.lookupAddress("Monas"))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "not json",
                "{}",
                "null",
                "[{\"lat\":\"NaN\",\"lon\":\"0\"}]",
                "[{\"lat\":\"91\",\"lon\":\"0\"}]",
                "[{}]"
            })
    void malformedResultsBecomeUnavailable(String body) {
        respond(200, body);
        assertThatThrownBy(() -> client.lookupAddress("Monas"))
                .isInstanceOf(ProviderUnavailableException.class);
    }
}
