package ru.eljke.driftguard.demo.kafka;

/**
 * Playback status for one Kafka demo producer.
 *
 * @param id producer identifier
 * @param service metric service name
 * @param metric metric name
 * @param operation operation or endpoint represented by the metric
 * @param producedPoints number of points already sent
 * @param totalPoints total points in the scenario
 * @param running whether the producer is still publishing
 */
public record KafkaProducerStatus(
        String id,
        String service,
        String metric,
        String operation,
        int producedPoints,
        int totalPoints,
        boolean running
) {
}


