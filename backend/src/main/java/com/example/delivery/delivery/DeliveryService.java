package com.example.delivery.delivery;

import com.example.delivery.config.FactoryLocation;
import com.example.delivery.geocoding.*;
import com.example.delivery.shared.NotFoundException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DeliveryService {
    private final DeliveryRepository deliveries;
    private final GeocodingService geocoding;
    private final FactoryLocation factory;

    public DeliveryService(DeliveryRepository deliveries, GeocodingService geocoding, FactoryLocation factory) {
        this.deliveries = deliveries;
        this.geocoding = geocoding;
        this.factory = factory;
    }

    public List<DeliveryResponse> list(Zone zone, DeliveryStatus status) {
        // Listing never calls the provider; persisted snapshots remain available during an outage.
        return deliveries.findFiltered(zone, status).stream().map(DeliveryResponse::from).toList();
    }

    public DeliveryResponse get(Long id) { return DeliveryResponse.from(require(id)); }

    public DeliveryResponse create(DeliveryRequest request) {
        Delivery delivery = new Delivery();
        applyRequest(delivery, request);
        applyGeocode(delivery, geocoding.resolve(delivery.destAddress));
        return DeliveryResponse.from(deliveries.saveAndFlush(delivery));
    }

    public DeliveryResponse update(Long id, DeliveryRequest request) {
        Delivery delivery = require(id);
        boolean addressChanged = !AddressNormalizer.normalize(delivery.destAddress)
                .equals(AddressNormalizer.normalize(request.destAddress()));
        applyRequest(delivery, request);
        // A status-only update keeps the existing snapshot, even if the API is down.
        if (addressChanged) applyGeocode(delivery, geocoding.resolve(delivery.destAddress));
        return DeliveryResponse.from(deliveries.saveAndFlush(delivery));
    }

    public DeliveryResponse retryGeocoding(Long id) {
        Delivery delivery = require(id);
        applyGeocode(delivery, geocoding.resolve(delivery.destAddress));
        return DeliveryResponse.from(deliveries.saveAndFlush(delivery));
    }

    public void delete(Long id) { deliveries.delete(require(id)); }

    private Delivery require(Long id) {
        return deliveries.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    private void applyRequest(Delivery delivery, DeliveryRequest request) {
        delivery.orderRef = request.orderRef().strip();
        delivery.destAddress = request.destAddress().strip();
        if (request.status() != null) delivery.status = request.status();
    }

    private void applyGeocode(Delivery delivery, GeocodeResult result) {
        delivery.geocodeSource = result.source();
        delivery.geocodedAt = result.fetchedAt();
        delivery.resolvedAddress = result.displayName();
        if (result.coordinates() == null) {
            delivery.destLat = null;
            delivery.destLng = null;
            delivery.distanceKm = null;
            delivery.zone = Zone.UNKNOWN;
            return;
        }
        double km = Haversine.distanceKm(factory.coordinates(), result.coordinates());
        // Classify BEFORE rounding: 49.999 km stays LOCAL, and 300.001 stays LONG_HAUL.
        delivery.zone = Zone.fromDistance(km);
        delivery.distanceKm = BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
        delivery.destLat = BigDecimal.valueOf(result.coordinates().latitude()).setScale(6, RoundingMode.HALF_UP);
        delivery.destLng = BigDecimal.valueOf(result.coordinates().longitude()).setScale(6, RoundingMode.HALF_UP);
    }
}
