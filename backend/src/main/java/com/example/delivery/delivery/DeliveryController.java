package com.example.delivery.delivery;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    private final DeliveryService service;
    public DeliveryController(DeliveryService service) { this.service = service; }

    @GetMapping
    public List<DeliveryResponse> list(@RequestParam(required = false) Zone zone,
                                      @RequestParam(required = false) DeliveryStatus status) {
        return service.list(zone, status);
    }

    @GetMapping("/{id}")
    public DeliveryResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse result = service.create(request);
        return ResponseEntity.created(URI.create("/api/deliveries/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    public DeliveryResponse update(@PathVariable Long id, @Valid @RequestBody DeliveryRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/geocode")
    public DeliveryResponse retry(@PathVariable Long id) { return service.retryGeocoding(id); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
