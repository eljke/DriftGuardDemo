package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

public record ResearchComparison(
        String scope,
        DemoDetectorProfile baselineProfile,
        int pairs,
        double meanAdaptiveUtility,
        double meanBaselineUtility,
        double meanDelta,
        double confidenceLow,
        double confidenceHigh,
        double wilcoxonPValue,
        int adaptiveWins,
        int adaptiveLosses,
        int ties
) {
}
