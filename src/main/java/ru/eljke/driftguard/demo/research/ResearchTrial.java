package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

public record ResearchTrial(
        String scenario,
        ResearchStrategy strategy,
        DemoDetectorProfile selectedProfile,
        long seed,
        double noiseMultiplier,
        double effectMultiplier,
        double precision,
        double recall,
        double f1,
        int falsePositiveEvents,
        double falsePositiveEventsPerThousand,
        long detectionDelaySamples,
        boolean detected
) {
}
