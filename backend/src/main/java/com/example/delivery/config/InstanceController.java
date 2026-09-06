package com.example.delivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!geocoder")
public class InstanceController {
    private final String instanceId;

    public InstanceController(@Value("${INSTANCE_ID:${HOSTNAME:local}}") String instanceId) {
        this.instanceId = instanceId;
    }

    /** Small diagnostic endpoint for observing which backend replica handled a request. */
    @GetMapping("/api/instance")
    public ResponseEntity<InstanceResponse> getInstance() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new InstanceResponse(instanceId));
    }

    public record InstanceResponse(String instanceId) {}
}
