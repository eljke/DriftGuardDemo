package ru.eljke.driftguard.demo.service;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Current operational state of the demo checkout service.
 *
 * @param running whether automatic traffic generation is active
 * @param mode active service behavior mode
 * @param operations total executed operations
 * @param successes successful operations
 * @param failures failed operations
 * @param errorRate recent error rate
 * @param meanLatencyMillis recent mean latency
 * @param queueSize current backlog size
 * @param throughputPerMinute recent throughput estimate
 * @param recentOperations latest business operation results
 * @param recentMetrics latest metric points sent to DriftGuard
 * @param recentAlerts recent DriftGuard alerts emitted by the service
 */
public record CheckoutServiceSnapshot(
        boolean running,
        CheckoutMode mode,
        long operations,
        long successes,
        long failures,
        double errorRate,
        double meanLatencyMillis,
        double queueSize,
        double throughputPerMinute,
        List<CheckoutOperationResult> recentOperations,
        List<MetricPoint> recentMetrics,
        List<DriftEvent> recentAlerts
) {
}
