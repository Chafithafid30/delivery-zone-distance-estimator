package com.example.delivery.delivery;

import com.example.delivery.config.FactoryLocation;
import com.example.delivery.geocoding.AddressResolver;
import com.example.delivery.geocoding.GeocodeResult;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!geocoder")
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final AddressResolver addressResolver;
    private final FactoryLocation factoryLocation;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            AddressResolver addressResolver,
            FactoryLocation factoryLocation) {
        this.deliveryRepository = deliveryRepository;
        this.addressResolver = addressResolver;
        this.factoryLocation = factoryLocation;
    }

    public List<DeliveryResponse> findDeliveries(Zone zone, DeliveryStatus status) {
        // Reads use persisted snapshots, so listing does not depend on the geocoder.
        return deliveryRepository.findFiltered(zone, status).stream()
                .map(DeliveryResponse::from)
                .toList();
    }

    public DeliveryResponse getDelivery(Long deliveryId) {
        return DeliveryResponse.from(findDeliveryOrThrow(deliveryId));
    }

    public DeliveryResponse createDelivery(DeliveryRequest request) {
        Delivery delivery =
                Delivery.create(request.orderRef(), request.destAddress(), request.status());
        updateDestinationLocation(delivery);
        return saveDelivery(delivery);
    }

    public DeliveryResponse updateDelivery(Long deliveryId, DeliveryRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        boolean destinationChanged = delivery.hasDestinationChanged(request.destAddress());
        delivery.updateDetails(request.orderRef(), request.destAddress(), request.status());

        // Status-only edits retain the original coordinates and fetch timestamp.
        if (destinationChanged) {
            updateDestinationLocation(delivery);
        }
        return saveDelivery(delivery);
    }

    public DeliveryResponse retryGeocoding(Long deliveryId) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        updateDestinationLocation(delivery);
        return saveDelivery(delivery);
    }

    public void deleteDelivery(Long deliveryId) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);
        deliveryRepository.delete(delivery);
    }

    private Delivery findDeliveryOrThrow(Long deliveryId) {
        return deliveryRepository
                .findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));
    }

    private void updateDestinationLocation(Delivery delivery) {
        GeocodeResult geocodeResult =
                addressResolver.resolveAddress(delivery.getDestinationAddress());
        delivery.updateDestination(geocodeResult, factoryLocation.coordinates());
    }

    private DeliveryResponse saveDelivery(Delivery delivery) {
        Delivery savedDelivery = deliveryRepository.saveAndFlush(delivery);
        return DeliveryResponse.from(savedDelivery);
    }
}
