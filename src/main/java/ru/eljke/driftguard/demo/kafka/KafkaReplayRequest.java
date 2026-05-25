package ru.eljke.driftguard.demo.kafka;

import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;

/**
 * Request body for replaying a synthetic scenario through Kafka.
 *
 * @param scenario scenario id to replay
 * @param speed playback speed multiplier
 * @param resetState whether detector state should be reset before replay
 * @param profile optional detector profile to activate
 * @param samples optional number of generated metric samples
 * @param baselineValue stable value before drift
 * @param driftValue value during step/drop/spike drift
 * @param noiseStdDev noise standard deviation
 * @param driftStartPercent drift start position as a percentage of stream length
 * @param spikeLengthPercent spike duration as a percentage of stream length
 */
public record KafkaReplayRequest(
        String scenario,
        double speed,
        boolean resetState,
        String profile,
        Integer samples,
        Double baselineValue,
        Double driftValue,
        Double noiseStdDev,
        Double driftStartPercent,
        Double spikeLengthPercent
) {
    public String normalizedScenario() {
        return scenario == null || scenario.isBlank() ? "latency-step" : scenario.trim();
    }

    public double normalizedSpeed() {
        return speed <= 0.0 ? 1.0 : Math.min(speed, 20.0);
    }

    public int normalizedSamples(int fallback) {
        int value = samples == null || samples <= 0 ? fallback : samples;
        return Math.max(80, Math.min(2000, value));
    }

    public DemoScenarioRequest scenarioRequest() {
        return new DemoScenarioRequest(
                samples,
                baselineValue,
                driftValue,
                noiseStdDev,
                driftStartPercent,
                spikeLengthPercent
        );
    }
}


