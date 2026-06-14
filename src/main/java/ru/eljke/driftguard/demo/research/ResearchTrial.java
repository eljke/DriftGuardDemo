package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;

public record ResearchTrial(
        String scenario,
        ResearchStrategy strategy,
        DetectorSensitivityProfile selectedProfile,
        long seed,
        double noiseMultiplier,
        double effectMultiplier,
        boolean driftExpected,
        Double precision,
        Double recall,
        Double f1,
        int falsePositiveEvents,
        double falsePositiveEventsPerThousand,
        Long detectionDelaySamples,
        Double specificity,
        boolean falseAlarmFree,
        Long timeToFirstFalseAlarmSamples,
        boolean detected
) {
}
