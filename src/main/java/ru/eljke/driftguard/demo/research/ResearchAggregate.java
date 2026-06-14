package ru.eljke.driftguard.demo.research;

import java.util.Map;

public record ResearchAggregate(
        String scenario,
        ResearchStrategy strategy,
        int trials,
        Double meanPrecision,
        Double meanRecall,
        Double meanF1,
        Double f1ConfidenceLow,
        Double f1ConfidenceHigh,
        double meanFalsePositiveEventsPerThousand,
        Double meanDetectionDelaySamples,
        Double detectionRate,
        Double meanSpecificity,
        double falseAlarmFreeRate,
        Double meanTimeToFirstFalseAlarmSamples,
        Map<String, Integer> selectedProfiles
) {
}
