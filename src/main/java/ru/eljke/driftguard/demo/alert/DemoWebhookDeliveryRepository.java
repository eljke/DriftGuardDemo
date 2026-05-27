package ru.eljke.driftguard.demo.alert;

import java.util.List;

/**
 * Stores webhook deliveries accepted by the demo incident-router endpoint.
 */
public interface DemoWebhookDeliveryRepository {
    void append(String channel, DemoWebhookAlertPayload payload);

    List<DemoWebhookDelivery> recent(int limit);
}
