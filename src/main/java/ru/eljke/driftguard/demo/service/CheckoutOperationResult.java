package ru.eljke.driftguard.demo.service;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;
import java.util.List;

/**
 * Result of one business operation executed by the demo service.
 *
 * @param id operation id
 * @param operation operation name
 * @param customerId customer id
 * @param success whether the operation succeeded
 * @param latencyMillis observed latency
 * @param queueSize current backlog size
 * @param mode service mode active during execution
 * @param occurredAt operation timestamp
 * @param alerts DriftGuard alerts emitted from operation metrics
 */
public record CheckoutOperationResult(
        long id,
        String operation,
        String customerId,
        boolean success,
        double latencyMillis,
        double queueSize,
        CheckoutMode mode,
        Instant occurredAt,
        List<DriftEvent> alerts
) {
}
