package ru.eljke.driftguard.demo;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.event.InMemoryDemoDriftEventRepository;
import ru.eljke.driftguard.demo.service.CheckoutMode;
import ru.eljke.driftguard.demo.service.CheckoutOperationRequest;
import ru.eljke.driftguard.demo.service.CheckoutService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutServiceTest {
    @Test
    void executeOperationPublishesMetricsIntoSnapshot() {
        CheckoutService service = newService();

        service.execute(new CheckoutOperationRequest("create-order", "customer-1"));

        var snapshot = service.snapshot();
        assertEquals(1, snapshot.operations());
        assertFalse(snapshot.recentOperations().isEmpty());
        assertEquals(4, snapshot.recentMetrics().size());
        assertTrue(snapshot.recentMetrics().stream()
                .allMatch(point -> "POST /checkout".equals(point.key().operation())));
        assertTrue(snapshot.recentMetrics().stream()
                .allMatch(point -> point.tags().containsKey("business-operation")));
    }

    @Test
    void serviceModeChangesOperationalBehavior() {
        CheckoutService service = newService();

        service.setMode(CheckoutMode.OUTAGE);
        service.execute(new CheckoutOperationRequest("authorize-payment", "customer-2"));

        var snapshot = service.snapshot();
        assertEquals(CheckoutMode.OUTAGE, snapshot.mode());
        assertTrue(snapshot.meanLatencyMillis() > 0.0);
    }

    @Test
    void metricHistoryCoversTheCurrentServiceSession() {
        CheckoutService service = newService();

        for (int index = 0; index < 60; index++) {
            service.execute(new CheckoutOperationRequest("create-order", "customer-" + index));
        }

        assertEquals(240, service.snapshot().recentMetrics().size());
    }

    @Test
    void resetHistoryStartsANewObservationSession() {
        CheckoutService service = newService();
        service.execute(new CheckoutOperationRequest("create-order", "customer-1"));

        var snapshot = service.resetHistory();

        assertEquals(0, snapshot.operations());
        assertEquals(0, snapshot.successes());
        assertEquals(0, snapshot.failures());
        assertTrue(snapshot.recentOperations().isEmpty());
        assertTrue(snapshot.recentMetrics().isEmpty());
        assertTrue(snapshot.recentAlerts().isEmpty());
    }

    @Test
    void degradedModeProducesDriftAlertsAfterShortWarmup() {
        DemoDetectionRuntime runtime = new DemoDetectionRuntime();
        CheckoutService service = new CheckoutService(
                runtime::detect,
                new InMemoryDemoDriftEventRepository(),
                new SimpleMeterRegistry()
        );

        for (int index = 0; index < 10; index++) {
            service.execute(new CheckoutOperationRequest("create-order", "normal-" + index));
        }
        service.setMode(CheckoutMode.DEGRADED);

        boolean alertEmitted = false;
        for (int index = 0; index < 8; index++) {
            var result = service.execute(new CheckoutOperationRequest("create-order", "degraded-" + index));
            alertEmitted = alertEmitted || !result.alerts().isEmpty();
        }

        assertTrue(alertEmitted);
    }

    private static CheckoutService newService() {
        return new CheckoutService(
                point -> java.util.List.of(),
                new InMemoryDemoDriftEventRepository(),
                new SimpleMeterRegistry()
        );
    }
}
