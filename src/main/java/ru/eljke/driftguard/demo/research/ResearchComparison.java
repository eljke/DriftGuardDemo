package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;

public record ResearchComparison(
        String scope,
        DetectorSensitivityProfile baselineProfile,
        int pairs,
        double meanAdaptiveUtility,
        double meanBaselineUtility,
        double meanDelta,
        double relativeImprovementPercent,
        double confidenceLow,
        double confidenceHigh,
        double wilcoxonPValue,
        int adaptiveWins,
        int adaptiveLosses,
        int ties
) {
}
