package ru.eljke.driftguard.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kafka settings used by the standalone demo playback and UI consumers.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo.kafka")
public class DemoKafkaProperties {
    /**
     * Enables endpoints that start Kafka-backed demo playback.
     */
    private boolean enabled = true;

    /**
     * Kafka bootstrap servers used by demo producers and consumers.
     */
    private String bootstrapServers = "localhost:9092";

    /**
     * Topic where synthetic metric points are published.
     */
    private String inputTopic = "driftguard.demo.metrics";

    /**
     * Topic where DriftGuard emits detected drift events.
     */
    private String outputTopic = "driftguard.demo.drift-events";

    /**
     * Kafka Streams application id for the demo topology.
     */
    private String applicationId = "driftguard-demo-streams";

    /**
     * Consumer group prefix used by the UI event reader.
     */
    private String consumerGroup = "driftguard-demo-ui";

    /**
     * Delay between published scenario points before playback speed is applied.
     */
    private Duration playbackInterval = Duration.ofMillis(150);
}


