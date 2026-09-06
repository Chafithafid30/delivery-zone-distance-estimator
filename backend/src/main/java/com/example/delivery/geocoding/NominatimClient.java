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
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String userAgent;
    private final Duration timeout;

    public NominatimClient(ObjectMapper mapper,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.user-agent}") String userAgent,
            @Value("${app.geocoding.timeout-seconds}") int timeoutSeconds) {
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.userAgent = userAgent;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public Optional<Place> lookup(String address) {
        URI uri = URI.create(baseUrl + "/search?q="
                + URLEncoder.encode(address, StandardCharsets.UTF_8) + "&format=json&limit=1");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout)
                .header("User-Agent", userAgent).header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ProviderUnavailableException("Geocoder returned HTTP " + response.statusCode());
            }
            JsonNode items = mapper.readTree(response.body());
            if (items == null || !items.isArray()) {
                throw new ProviderUnavailableException("Unexpected geocoder response");
            }
            if (items.isEmpty()) return Optional.empty();
            JsonNode item = items.get(0);
            Coordinates coordinates = new Coordinates(
                    Double.parseDouble(item.path("lat").asText()),
                    Double.parseDouble(item.path("lon").asText()));
            String displayName = item.path("display_name").asText(address);
            return Optional.of(new Place(coordinates, displayName.substring(0, Math.min(500, displayName.length()))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderUnavailableException("Geocoding interrupted", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new ProviderUnavailableException("Geocoder is unavailable or returned invalid data", e);
        }
    }
}
