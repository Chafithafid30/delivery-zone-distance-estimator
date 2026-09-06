package com.example.delivery.geocoding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Request timing state, accessed only while GeocodingService holds its outbound lock. */
@Component
@ConditionalOnProperty(name = "app.geocoding.mode", havingValue = "local", matchIfMissing = true)
public class GeocodingRequestPolicy {
    private static final long MINIMUM_REQUEST_INTERVAL_MS = 1000;

    private final long requestIntervalNanos;
    private final long failureCooldownNanos;
    private long lastRequestCompletedAtNanos;
    private long lastFailureAtNanos;
    private boolean hasCompletedRequest;
    private boolean isCoolingDown;

    public GeocodingRequestPolicy(
            @Value("${app.geocoding.interval-ms}") long requestIntervalMs,
            @Value("${app.geocoding.cooldown-seconds}") long failureCooldownSeconds) {
        requestIntervalNanos =
                TimeUnit.MILLISECONDS.toNanos(
                        Math.max(MINIMUM_REQUEST_INTERVAL_MS, requestIntervalMs));
        failureCooldownNanos = TimeUnit.SECONDS.toNanos(Math.max(0, failureCooldownSeconds));
    }

    boolean isInCooldown() {
        long elapsedSinceFailureNanos = System.nanoTime() - lastFailureAtNanos;
        return isCoolingDown && elapsedSinceFailureNanos < failureCooldownNanos;
    }

    void awaitNextRequest() throws InterruptedException {
        if (!hasCompletedRequest) {
            return;
        }
        long elapsedSinceRequestNanos = System.nanoTime() - lastRequestCompletedAtNanos;
        long remainingWaitNanos = requestIntervalNanos - elapsedSinceRequestNanos;
        if (remainingWaitNanos > 0) {
            TimeUnit.NANOSECONDS.sleep(remainingWaitNanos);
        }
    }

    void recordSuccess() {
        isCoolingDown = false;
    }

    void recordFailure() {
        lastFailureAtNanos = System.nanoTime();
        isCoolingDown = true;
    }

    void recordRequestCompleted() {
        lastRequestCompletedAtNanos = System.nanoTime();
        hasCompletedRequest = true;
    }
}
