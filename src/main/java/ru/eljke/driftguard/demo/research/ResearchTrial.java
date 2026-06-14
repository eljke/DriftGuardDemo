package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

public record ResearchTrial(
        String scenario,
        ResearchStrategy strategy,
        DemoDetectorProfile selectedProfile,
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
