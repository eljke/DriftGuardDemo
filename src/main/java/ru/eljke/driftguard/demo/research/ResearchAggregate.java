package ru.eljke.driftguard.demo.research;

import java.util.Map;

public record ResearchAggregate(
        String scenario,
        ResearchStrategy strategy,
        int trials,
        double meanPrecision,
        double meanRecall,
        double meanF1,
        double f1ConfidenceLow,
        double f1ConfidenceHigh,
        double meanFalsePositiveEventsPerThousand,
        double meanDetectionDelaySamples,
        double detectionRate,
        Map<String, Integer> selectedProfiles
) {
}
