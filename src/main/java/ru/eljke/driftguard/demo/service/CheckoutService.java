package ru.eljke.driftguard.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.event.DemoDriftEventRepository;
import ru.eljke.driftguard.demo.event.DemoStoredDriftEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Small checkout service that emits operational metrics into DriftGuard.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private static final String SERVICE = "checkout-service";
    private static final List<String> OPERATIONS = List.of(
            "create-order",
            "authorize-payment",
            "reserve-inventory",
            "dispatch-order"
    );
    private static final int OPERATION_HISTORY_LIMIT = 220;
    private static final int SERVICE_EVENT_LIMIT = 2000;

    private final DemoDetectionRuntime detectionRuntime;
    private final DemoDriftEventRepository eventRepository;
    private final MeterRegistry meterRegistry;
    private final Random random = new Random(42L);
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "checkout-service-traffic");
        thread.setDaemon(true);
        return thread;
    });

    private final List<CheckoutOperationResult> recentOperations = new CopyOnWriteArrayList<>();
    private final List<MetricPoint> recentMetrics = new CopyOnWriteArrayList<>();
    private volatile ScheduledFuture<?> trafficTask;
    private volatile CheckoutMode mode = CheckoutMode.NORMAL;
    private volatile double queueSize = 12.0;
    private volatile long successes;
    private volatile long failures;

    public CheckoutServiceSnapshot snapshot() {
        List<CheckoutOperationResult> operations = latest(recentOperations, 40);
        List<MetricPoint> metrics = List.copyOf(recentMetrics);
        List<DriftEvent> alerts = eventRepository.recent(SERVICE_EVENT_LIMIT).stream()
                .filter(stored -> "service".equals(stored.source()))
                .map(DemoStoredDriftEvent::event)
                .toList();
        long total = successes + failures;
        return new CheckoutServiceSnapshot(
                running.get(),
                mode,
                total,
                successes,
                failures,
                ratio(failures, Math.max(1, total)),
                meanLatency(operations),
                queueSize,
                throughputPerMinute(operations),
                operations,
                metrics,
                alerts
        );
    }

    public CheckoutOperationResult execute(CheckoutOperationRequest request) {
        CheckoutOperationRequest safeRequest = request == null ? new CheckoutOperationRequest(null, null) : request;
        String operation = normalizeOperation(safeRequest.normalizedOperation());
        Instant now = Instant.now();
        CheckoutMode currentMode = mode;
        double latency = latency(operation, currentMode);
        boolean success = random.nextDouble() >= errorProbability(currentMode);
        queueSize = nextQueueSize(currentMode, success);

        if (success) {
            successes++;
        } else {
            failures++;
        }

        List<MetricPoint> points = metricPoints(operation, now, latency, success);
        List<DriftEvent> alerts = new ArrayList<>();
        points.forEach(point -> {
            recentMetrics.add(point);
            List<DriftEvent> detected = detectionRuntime.detect(point);
            alerts.addAll(detected);
            eventRepository.appendAll("service", "checkout-service", detected);
        });

        CheckoutOperationResult result = new CheckoutOperationResult(
                sequence.incrementAndGet(),
                operation,
                safeRequest.normalizedCustomerId(),
                success,
                latency,
                queueSize,
                currentMode,
                now,
                List.copyOf(alerts)
        );
        recentOperations.add(result);
        trim(recentOperations, OPERATION_HISTORY_LIMIT);
        recordMicrometer(result);
        return result;
    }

    public CheckoutServiceSnapshot startTraffic() {
        if (running.compareAndSet(false, true)) {
            trafficTask = executor.scheduleAtFixedRate(
                    () -> execute(new CheckoutOperationRequest(randomOperation(), "auto-" + sequence.get())),
                    0,
                    350,
                    TimeUnit.MILLISECONDS
            );
        }
        return snapshot();
    }

    public CheckoutServiceSnapshot stopTraffic() {
        running.set(false);
        ScheduledFuture<?> task = trafficTask;
        if (task != null) {
            task.cancel(false);
            trafficTask = null;
        }
        return snapshot();
    }

    public CheckoutServiceSnapshot setMode(CheckoutMode mode) {
        this.mode = mode == null ? CheckoutMode.NORMAL : mode;
        return snapshot();
    }

    public List<String> operations() {
        return OPERATIONS;
    }

    @PreDestroy
    public void shutdown() {
        stopTraffic();
        executor.shutdownNow();
    }

    private List<MetricPoint> metricPoints(String operation, Instant now, double latency, boolean success) {
        return List.of(
                point("latency", operation, now, latency, MetricKind.DURATION),
                point("error-rate", operation, now, success ? 0.0 : 1.0, MetricKind.RATE),
                point("throughput", operation, now, throughputValue(success), MetricKind.RATE),
                point("queue-size", "orders.created", now, queueSize, MetricKind.SIZE)
        );
    }

    private MetricPoint point(String metric, String operation, Instant now, double value, MetricKind kind) {
        return MetricPoint.builder()
                .key(MetricKey.builder()
                        .service(SERVICE)
                        .metric(metric)
                        .instance("checkout-api-1")
                        .operation(operation)
                        .build())
                .timestamp(now)
                .value(value)
                .kind(kind)
                .tags(Map.of("mode", mode.name(), "component", "checkout"))
                .attributes(Map.of("source", "business-operation"))
                .build();
    }

    private double latency(String operation, CheckoutMode currentMode) {
        double base = switch (operation) {
            case "authorize-payment" -> 145.0;
            case "reserve-inventory" -> 95.0;
            case "dispatch-order" -> 80.0;
            default -> 110.0;
        };
        double multiplier = switch (currentMode) {
            case NORMAL -> 1.0;
            case DEGRADED -> 2.45;
            case OUTAGE -> 4.2;
        };
        return Math.max(10.0, base * multiplier + random.nextGaussian() * 9.0);
    }

    private double errorProbability(CheckoutMode currentMode) {
        return switch (currentMode) {
            case NORMAL -> 0.015;
            case DEGRADED -> 0.16;
            case OUTAGE -> 0.42;
        };
    }

    private double nextQueueSize(CheckoutMode currentMode, boolean success) {
        double pressure = switch (currentMode) {
            case NORMAL -> -1.2;
            case DEGRADED -> 3.5;
            case OUTAGE -> 8.0;
        };
        double next = queueSize + pressure + (success ? -0.4 : 2.0) + random.nextGaussian();
        return Math.clamp(next, 0.0, 1_000.0);
    }

    private double throughputValue(boolean success) {
        double baseline = switch (mode) {
            case NORMAL -> 950.0;
            case DEGRADED -> 620.0;
            case OUTAGE -> 260.0;
        };
        return Math.max(0.0, baseline + (success ? 0.0 : -90.0) + random.nextGaussian() * 35.0);
    }

    private void recordMicrometer(CheckoutOperationResult result) {
        Counter.builder("checkout.operations")
                .tag("operation", result.operation())
                .tag("success", Boolean.toString(result.success()))
                .tag("mode", result.mode().name())
                .register(meterRegistry)
                .increment();
        Timer.builder("checkout.operation.latency")
                .tag("operation", result.operation())
                .tag("mode", result.mode().name())
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.round(result.latencyMillis())));
    }

    private String normalizeOperation(String operation) {
        return OPERATIONS.contains(operation) ? operation : "create-order";
    }

    private String randomOperation() {
        return OPERATIONS.get(random.nextInt(OPERATIONS.size()));
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : numerator * 1.0 / denominator;
    }

    private static double meanLatency(List<CheckoutOperationResult> operations) {
        return operations.stream()
                .mapToDouble(CheckoutOperationResult::latencyMillis)
                .average()
                .orElse(0.0);
    }

    private static double throughputPerMinute(List<CheckoutOperationResult> operations) {
        if (operations.size() < 2) {
            return operations.size();
        }
        Instant first = operations.stream().map(CheckoutOperationResult::occurredAt).min(Comparator.naturalOrder()).orElse(Instant.now());
        Instant last = operations.stream().map(CheckoutOperationResult::occurredAt).max(Comparator.naturalOrder()).orElse(first);
        double minutes = Math.max(1.0 / 60.0, Duration.between(first, last).toMillis() / 60_000.0);
        return operations.size() / minutes;
    }

    private static <T> List<T> latest(List<T> source, int limit) {
        int from = Math.max(0, source.size() - limit);
        return List.copyOf(source.subList(from, source.size()));
    }

    private static void trim(List<?> source, int limit) {
        int overflow = source.size() - limit;
        for (int index = 0; index < overflow; index++) {
            source.remove(0);
        }
    }
}
