package ru.eljke.driftguard.demo.config;

import java.time.Duration;
import java.util.List;

/**
 * Configuration snapshot displayed by the demo UI.
 *
 * @param aggressiveness active detector sensitivity profile
 * @param availableProfiles supported profile names
 * @param registeredAlgorithms algorithms available through the DriftGuard registry
 * @param kafka effective Kafka demo settings
 * @param detectors active detector definitions
 */
public record DemoConfigurationView(
        AggressivenessView aggressiveness,
        List<String> availableProfiles,
        List<String> registeredAlgorithms,
        KafkaConfigurationView kafka,
        List<DetectorConfigurationView> detectors
) {
    /**
     * Human-readable description of the active sensitivity profile.
     *
     * @param level profile label
     * @param description profile behavior summary
     */
    public record AggressivenessView(String level, String description) {
    }

    /**
     * Effective Kafka configuration used by demo playback.
     *
     * @param demoEnabled whether Kafka playback endpoints are enabled
     * @param bootstrapServers Kafka bootstrap servers
     * @param inputTopic topic receiving metric points
     * @param outputTopic topic receiving drift events
     * @param applicationId Kafka Streams application id
     * @param playbackInterval base delay between published scenario points
     */
    public record KafkaConfigurationView(
            boolean demoEnabled,
            String bootstrapServers,
            String inputTopic,
            String outputTopic,
            String applicationId,
            Duration playbackInterval
    ) {
    }

    /**
     * UI-facing view of one active detector definition.
     *
     * @param name detector name
     * @param algorithm algorithm id
     * @param services selected services
     * @param metrics selected metric names
     * @param warningThreshold warning threshold for threshold-based detectors
     * @param criticalThreshold critical threshold for threshold-based detectors
     * @param warningPValue warning p-value for statistical detectors
     * @param criticalPValue critical p-value for statistical detectors
     * @param warmupSamples samples required before detection starts
     * @param emissionPolicy policy that suppresses noisy repeated alerts
     * @param sensitivity active profile and runtime version
     */
    public record DetectorConfigurationView(
            String name,
            String algorithm,
            List<String> services,
            List<String> metrics,
            double warningThreshold,
            double criticalThreshold,
            double warningPValue,
            double criticalPValue,
            int warmupSamples,
            EmissionPolicyView emissionPolicy,
            String sensitivity
    ) {
    }

    /**
     * Alert emission policy shown in detector configuration.
     *
     * @param minConsecutiveSignals required consecutive signals before alerting
     * @param cooldown minimum time between repeated alerts
     * @param recoveryConsecutiveNormal normal samples required to leave drift state
     */
    public record EmissionPolicyView(int minConsecutiveSignals, Duration cooldown, int recoveryConsecutiveNormal) {
    }
}


