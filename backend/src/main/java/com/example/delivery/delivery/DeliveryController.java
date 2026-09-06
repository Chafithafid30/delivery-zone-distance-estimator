package com.example.delivery.delivery;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public List<DeliveryResponse> listDeliveries(
            @RequestParam(required = false) Zone zone,
            @RequestParam(required = false) DeliveryStatus status) {
        return deliveryService.findDeliveries(zone, status);
    }

    @GetMapping("/{id}")
    public DeliveryResponse getDelivery(@PathVariable("id") Long deliveryId) {
        return deliveryService.getDelivery(deliveryId);
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> createDelivery(
            @Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse createdDelivery = deliveryService.createDelivery(request);
        URI deliveryLocation = URI.create("/api/deliveries/" + createdDelivery.id());
        return ResponseEntity.created(deliveryLocation).body(createdDelivery);
    }

    @PutMapping("/{id}")
    public DeliveryResponse updateDelivery(
            @PathVariable("id") Long deliveryId, @Valid @RequestBody DeliveryRequest request) {
        return deliveryService.updateDelivery(deliveryId, request);
    }

    @PostMapping("/{id}/geocode")
    public DeliveryResponse retryGeocoding(@PathVariable("id") Long deliveryId) {
        return deliveryService.retryGeocoding(deliveryId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelivery(@PathVariable("id") Long deliveryId) {
        deliveryService.deleteDelivery(deliveryId);
        return ResponseEntity.noContent().build();
    }
}
