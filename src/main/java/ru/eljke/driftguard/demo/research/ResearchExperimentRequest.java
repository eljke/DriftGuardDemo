package ru.eljke.driftguard.demo.research;

import java.util.List;

public record ResearchExperimentRequest(
        Integer repetitions,
        Integer samples,
        Long baseSeed,
        List<String> scenarios,
        List<Double> noiseMultipliers,
        List<Double> effectMultipliers
) {
    private static final List<String> DEFAULT_SCENARIOS = List.of(
            "latency-step",
            "error-rate-spike",
            "throughput-drop",
            "queue-growth",
            "seasonal-latency"
    );

    public ResearchExperimentRequest normalized() {
        return new ResearchExperimentRequest(
                bounded(repetitions, 10, 2, 100),
                bounded(samples, 200, 100, 2_000),
                baseSeed == null ? 1_000L : baseSeed,
                scenarios == null || scenarios.isEmpty() ? DEFAULT_SCENARIOS : List.copyOf(scenarios),
                values(noiseMultipliers, List.of(0.5, 1.0, 2.0)),
                values(effectMultipliers, List.of(0.75, 1.0, 1.25))
        );
    }

    public int totalTrials() {
        ResearchExperimentRequest request = normalized();
        int cells = request.scenarios().size()
                * request.noiseMultipliers().size()
                * request.effectMultipliers().size();
        return cells * (
                request.calibrationRepetitions() * ResearchStrategy.fixed().size()
                        + request.holdoutRepetitions() * ResearchStrategy.values().length
        );
    }

    public int calibrationRepetitions() {
        return Math.max(1, normalized().repetitions() / 3);
    }

    public int holdoutRepetitions() {
        ResearchExperimentRequest request = normalized();
        return request.repetitions() - request.calibrationRepetitions();
    }

    private static int bounded(Integer value, int fallback, int min, int max) {
        return Math.max(min, Math.min(max, value == null ? fallback : value));
    }

    private static List<Double> values(List<Double> values, List<Double> fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return values.stream()
                .map(value -> Math.max(0.1, Math.min(5.0, value)))
                .distinct()
                .toList();
    }
}
