package ru.eljke.driftguard.demo.event;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;

/**
 * Drift event enriched with demo storage metadata.
 *
 * @param source scenario source, such as synthetic, live or kafka
 * @param runId scenario run identifier
 * @param receivedAt time when the demo stored the event
 * @param event DriftGuard drift event payload
 */
public record DemoStoredDriftEvent(
        String source,
        String runId,
        Instant receivedAt,
        DriftEvent event
) {
    public DemoStoredDriftEvent {
        source = source == null || source.isBlank() ? "unknown" : source;
        runId = runId == null || runId.isBlank() ? "unknown" : runId;
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}

