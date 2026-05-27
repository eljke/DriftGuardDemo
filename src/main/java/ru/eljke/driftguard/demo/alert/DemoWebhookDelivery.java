package ru.eljke.driftguard.demo.alert;

import java.time.Instant;

/**
 * Stored fact that the demo incident-router endpoint accepted a webhook alert.
 *
 * @param acceptedAt when the local receiver accepted the webhook
 * @param channel demo delivery channel header
 * @param payload alert payload sent by DriftGuard's webhook sink
 */
public record DemoWebhookDelivery(
        Instant acceptedAt,
        String channel,
        DemoWebhookAlertPayload payload
) {
}
