package ru.eljke.driftguard.demo.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Current state of Kafka-backed demo playback.
 *
 * @param enabled whether Kafka demo endpoints are enabled
 * @param running whether playback is active
 * @param replay whether the current run is replay mode
 * @param scenario active scenario id
 * @param inputTopic topic receiving metric points
 * @param outputTopic topic receiving drift events
 * @param speed playback speed multiplier
 * @param bootstrapServers Kafka bootstrap servers
 * @param producedPoints number of metric points already produced
 * @param totalPoints total planned metric points
 * @param producers per-producer playback states
 * @param consumedEvents drift events consumed by the demo UI
 * @param samplePoints recently produced sample points
 * @param error latest playback error, if any
 */
public record KafkaDemoStatus(
        boolean enabled,
        boolean running,
        boolean replay,
        String scenario,
        String inputTopic,
        String outputTopic,
        double speed,
        String bootstrapServers,
        int producedPoints,
        int totalPoints,
        List<KafkaProducerStatus> producers,
        List<DriftEvent> consumedEvents,
        List<MetricPoint> samplePoints,
        String error
) {
}


