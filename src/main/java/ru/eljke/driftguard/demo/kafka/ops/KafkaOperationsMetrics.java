package ru.eljke.driftguard.demo.kafka.ops;

/**
 * Aggregated Micrometer counters and timers for Kafka detection processing.
 *
 * @param processedPoints number of metric points processed by the topology
 * @param emittedEvents number of drift events emitted by the topology
 * @param failedPoints number of points that failed processing
 * @param routedErrors number of failures routed to the error topic
 * @param durationMeasurements number of recorded processing-duration samples
 * @param totalDurationMillis total recorded processing duration in milliseconds
 * @param maxDurationMillis maximum recorded processing duration in milliseconds
 * @param meanDurationMillis average processing duration in milliseconds
 */
public record KafkaOperationsMetrics(
        double processedPoints,
        double emittedEvents,
        double failedPoints,
        double routedErrors,
        long durationMeasurements,
        double totalDurationMillis,
        double maxDurationMillis,
        double meanDurationMillis
) {
    public static KafkaOperationsMetrics empty() {
        return new KafkaOperationsMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0);
    }
}


