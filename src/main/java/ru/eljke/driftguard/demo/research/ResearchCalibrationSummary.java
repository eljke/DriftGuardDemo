package ru.eljke.driftguard.demo.research;

import java.util.Map;

public record ResearchCalibrationSummary(
        int calibrationRepetitions,
        int holdoutRepetitions,
        int calibrationTrials,
        int evaluatedTrials,
        int trainingExamples,
        Map<String, Integer> bestProfileLabels
) {
}
