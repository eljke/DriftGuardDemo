package ru.eljke.driftguard.demo.kafka.ops;

import java.util.List;

/**
 * Combined UI view of Kafka playback, Streams topology and telemetry state.
 *
 * @param enabled whether Kafka demo endpoints are enabled
 * @param running whether playback is active
 * @param replay whether the current run is replay mode
 * @param scenario active scenario id
 * @param inputTopic topic receiving metric points
 * @param outputTopic topic receiving drift events
 * @param bootstrapServers Kafka bootstrap servers
 * @param producedPoints number of metric points already produced
 * @param totalPoints total planned metric points
 * @param consumedEvents number of consumed drift events
 * @param progressPercent playback progress from 0 to 100
 * @param streamsApplicationId Kafka Streams application id
 * @param streamsInputTopics topology input topics
 * @param streamsOutputTopic topology output topic
 * @param runtimeStateStoreName Kafka state store name for detector runtime state
 * @param detectionErrorMode configured behavior for processing failures
 * @param telemetryEnabled whether Micrometer telemetry is available
 * @param metrics aggregated topology metrics
 * @param error latest playback error, if any
 */
public record KafkaOperationsSnapshot(
        boolean enabled,
        boolean running,
        boolean replay,
        String scenario,
        String inputTopic,
        String outputTopic,
        String bootstrapServers,
        int producedPoints,
        int totalPoints,
        int consumedEvents,
        double progressPercent,
        String streamsApplicationId,
        List<String> streamsInputTopics,
        String streamsOutputTopic,
        String runtimeStateStoreName,
        String detectionErrorMode,
        boolean telemetryEnabled,
        KafkaOperationsMetrics metrics,
        String error
) {
}


