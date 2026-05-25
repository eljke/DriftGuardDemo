package ru.eljke.driftguard.demo.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.eljke.driftguard.core.alert.DriftAlert;
import ru.eljke.driftguard.core.alert.DriftAlertSink;
import ru.eljke.driftguard.demo.event.DemoDriftEventRepository;

/**
 * Example application-specific alert sink.
 *
 * <p>The DriftGuard starter also creates the default SLF4J alert sink. This
 * sink shows how a production service can add a second delivery channel; the
 * demo stores alerts so the UI can render recent incidents.</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class RepositoryDriftAlertSink implements DriftAlertSink {
    private static final String SOURCE = "service";
    private static final String RUN_ID = "checkout-service";

    private final DemoDriftEventRepository eventRepository;

    @Override
    public void accept(DriftAlert alert) {
        eventRepository.append(SOURCE, RUN_ID, alert.event());
    }
}
