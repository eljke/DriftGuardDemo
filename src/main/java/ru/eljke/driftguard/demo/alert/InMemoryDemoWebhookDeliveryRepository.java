package ru.eljke.driftguard.demo.alert;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory webhook delivery store for the standalone demo.
 */
@Repository
public class InMemoryDemoWebhookDeliveryRepository implements DemoWebhookDeliveryRepository {
    private static final int MAX_DELIVERIES = 1000;

    private final List<DemoWebhookDelivery> deliveries = new CopyOnWriteArrayList<>();

    @Override
    public void append(String channel, DemoWebhookAlertPayload payload) {
        if (payload == null) {
            return;
        }
        deliveries.add(new DemoWebhookDelivery(Instant.now(), channel, payload));
        trim();
    }

    @Override
    public List<DemoWebhookDelivery> recent(int limit) {
        int safeLimit = Math.max(1, limit);
        return deliveries.stream()
                .sorted(Comparator.comparing(DemoWebhookDelivery::acceptedAt).reversed())
                .limit(safeLimit)
                .toList();
    }

    private void trim() {
        int overflow = deliveries.size() - MAX_DELIVERIES;
        for (int index = 0; index < overflow; index++) {
            if (deliveries.isEmpty()) {
                return;
            }
            deliveries.removeFirst();
        }
    }
}
