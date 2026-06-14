package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

import java.util.Map;

public record ResearchCalibrationSummary(
        int calibrationRepetitions,
        int holdoutRepetitions,
        int calibrationTrials,
        int evaluatedTrials,
        int trainingExamples,
        DemoDetectorProfile bestGlobalProfile,
        Map<String, Integer> bestProfileLabels
) {
}
