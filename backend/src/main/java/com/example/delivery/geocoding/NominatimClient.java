package com.example.delivery.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
public class NominatimClient implements GeocodingProvider {
    private static final int MAX_DISPLAY_NAME_LENGTH = 500;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String userAgent;
    private final Duration requestTimeout;

    public NominatimClient(
            ObjectMapper objectMapper,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.user-agent}") String userAgent,
            @Value("${app.geocoding.timeout-seconds}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.userAgent = userAgent;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    @Override
    public Optional<Place> lookupAddress(String address) {
        HttpRequest searchRequest = buildSearchRequest(address);
        try {
            HttpResponse<String> response =
                    httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ProviderUnavailableException(
                        "Geocoder returned HTTP " + response.statusCode());
            }
            return readSearchResult(response.body(), address);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProviderUnavailableException("Geocoding interrupted", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ProviderUnavailableException(
                    "Geocoder is unavailable or returned invalid data", exception);
        }
    }

    private HttpRequest buildSearchRequest(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        URI searchUri =
                URI.create(baseUrl + "/search?q=" + encodedAddress + "&format=json&limit=1");
        return HttpRequest.newBuilder(searchUri)
                .timeout(requestTimeout)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private Optional<Place> readSearchResult(String responseBody, String requestedAddress)
            throws IOException {
        JsonNode searchResults = objectMapper.readTree(responseBody);
        if (searchResults == null || !searchResults.isArray()) {
            throw new ProviderUnavailableException("Unexpected geocoder response");
        }
        if (searchResults.isEmpty()) {
            return Optional.empty();
        }

        JsonNode firstResult = searchResults.get(0);
        Coordinates coordinates =
                new Coordinates(
                        Double.parseDouble(firstResult.path("lat").asText()),
                        Double.parseDouble(firstResult.path("lon").asText()));
        String displayName = firstResult.path("display_name").asText(requestedAddress);
        String boundedDisplayName =
                displayName.substring(0, Math.min(MAX_DISPLAY_NAME_LENGTH, displayName.length()));
        return Optional.of(new Place(coordinates, boundedDisplayName));
    }
}
