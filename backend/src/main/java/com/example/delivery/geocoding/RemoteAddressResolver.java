package com.example.delivery.geocoding;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.geocoding.mode", havingValue = "remote")
public class RemoteAddressResolver implements AddressResolver {
    private final GeocodeCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI geocodingEndpoint;
    private final Duration requestTimeout;

    public RemoteAddressResolver(
            GeocodeCacheRepository cacheRepository,
            ObjectMapper objectMapper,
            @Value("${app.geocoding.remote-url}") String serviceUrl,
            @Value("${app.geocoding.remote-timeout-seconds}") int timeoutSeconds) {
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.geocodingEndpoint =
                URI.create(serviceUrl.replaceAll("/+$", "") + "/internal/geocoding");
    }

    @Override
    public GeocodeResult resolveAddress(String address) {
        String normalizedAddress = AddressNormalizer.normalize(address);
        Optional<GeocodeResult> cachedResult = findCachedResult(normalizedAddress);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        try {
            String requestBody =
                    objectMapper.writeValueAsString(Map.of("address", normalizedAddress));
            HttpRequest request =
                    HttpRequest.newBuilder(geocodingEndpoint)
                            .timeout(requestTimeout)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                GeocodeResult result = objectMapper.readValue(response.body(), GeocodeResult.class);
                if (isValidResult(result)) {
                    return result;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | IllegalArgumentException exception) {
            // The board can still create UNKNOWN deliveries when the shared worker is down.
        }
        // A timed-out worker may have committed the coordinates while this caller was waiting.
        return findCachedResult(normalizedAddress)
                .orElseGet(() -> GeocodeResult.unknown(GeocodeResult.Source.UNAVAILABLE));
    }

    private boolean isValidResult(GeocodeResult result) {
        if (result == null || result.source() == null) {
            return false;
        }
        if (result.coordinates() == null) {
            return result.source() == GeocodeResult.Source.UNAVAILABLE
                    || result.source() == GeocodeResult.Source.NOT_FOUND;
        }
        return result.fetchedAt() != null
                && (result.source() == GeocodeResult.Source.CACHE
                        || result.source() == GeocodeResult.Source.NOMINATIM);
    }

    private Optional<GeocodeResult> findCachedResult(String normalizedAddress) {
        return cacheRepository
                .findByNormalizedAddress(normalizedAddress)
                .map(entry -> GeocodeResult.from(entry, GeocodeResult.Source.CACHE));
    }
}
