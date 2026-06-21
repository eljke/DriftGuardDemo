package ru.eljke.driftguard.demo.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.demo.event.InMemoryDemoDriftEventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CheckoutServiceMicrometerTest {
    @Test
    void registersQueueGaugeForMicrometerInput() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CheckoutService service = new CheckoutService(
                point -> java.util.List.of(),
                new InMemoryDemoDriftEventRepository(),
                meterRegistry
        );

        service.registerMicrometerMeters();
        service.execute(new CheckoutOperationRequest("create-order", "customer-1"));

        Gauge gauge = meterRegistry.find("checkout.queue.size")
                .tag("operation", "POST /checkout")
                .tag("component", "checkout")
                .gauge();
        assertNotNull(gauge);
        assertEquals(service.currentQueueSize(), gauge.value(), 0.0001);
    }
}
