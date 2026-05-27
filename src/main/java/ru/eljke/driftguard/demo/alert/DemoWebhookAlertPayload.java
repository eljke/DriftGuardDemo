package ru.eljke.driftguard.demo.alert;

import java.util.Map;

/**
 * JSON payload accepted by the demo webhook endpoint.
 *
 * <p>The shape matches the default DriftGuard webhook sink payload and models
 * a small incident-router contract used by a production service.</p>
 */
public record DemoWebhookAlertPayload(
        String id,
        String severity,
        String title,
        String message,
        String service,
        String metric,
        String operation,
        Map<String, String> labels
) {
}
