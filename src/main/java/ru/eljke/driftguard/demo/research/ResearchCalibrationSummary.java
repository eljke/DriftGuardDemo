package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;

import java.util.Map;

public record ResearchCalibrationSummary(
        int calibrationRepetitions,
        int holdoutRepetitions,
        int calibrationTrials,
        int evaluatedTrials,
        int trainingExamples,
        DetectorSensitivityProfile bestGlobalProfile,
        Map<String, Integer> bestProfileLabels
) {
}
