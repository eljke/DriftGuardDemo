package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.adaptive.BaselineCharacteristics;
import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;
import ru.eljke.driftguard.demo.research.ResearchExperimentEngine;
import ru.eljke.driftguard.demo.research.ResearchExperimentRequest;
import ru.eljke.driftguard.demo.research.ResearchExperimentReport;
import ru.eljke.driftguard.demo.research.ResearchStrategy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchExperimentEngineTest {
    @Test
    void experimentProducesPairedStrategyResults() {
        ResearchExperimentRequest request = new ResearchExperimentRequest(
                2,
                120,
                50L,
                List.of("latency-step", "seasonal-latency"),
                List.of(1.0),
                List.of(1.0)
        );

        ResearchExperimentReport report = new ResearchExperimentEngine().run(request, ignored -> { }, () -> false);

        assertEquals(8, report.totalTrials());
        assertEquals(8, report.aggregates().size());
        assertEquals(6, report.calibration().calibrationTrials());
        assertEquals(2, report.calibration().trainingExamples());
        assertEquals(3, report.comparisons().size());
        assertEquals(2, report.comparisons().getFirst().pairs());
        var overall = report.comparisons().getFirst();
        assertEquals(
                overall.meanDelta() / overall.meanBaselineUtility() * 100.0,
                overall.relativeImprovementPercent(),
                1.0e-12
        );
        assertTrue(report.aggregates().stream().allMatch(result -> result.trials() == 1));
        assertTrue(report.aggregates().stream()
                .filter(result -> result.meanF1() != null)
                .allMatch(result ->
                        result.f1ConfidenceLow() <= result.meanF1()
                                && result.meanF1() <= result.f1ConfidenceHigh()
        ));
        var seasonal = report.aggregates().stream()
                .filter(result -> result.scenario().equals("seasonal-latency"))
                .toList();
        assertTrue(seasonal.stream().allMatch(result -> result.meanSpecificity() != null));
        assertTrue(seasonal.stream().allMatch(result ->
                result.meanSpecificity() >= result.falseAlarmFreeRate()
        ));
        seasonal.forEach(result -> {
            assertNull(result.meanPrecision());
            assertNull(result.meanRecall());
            assertNull(result.meanF1());
            assertNull(result.meanDetectionDelaySamples());
            assertNull(result.detectionRate());
        });
    }

    @Test
    void calibratedSelectorUsesNearestRobustBaseline() {
        BaselineCharacteristics errorRate = characteristics(0.01, 0.002, 0.2, 0.1, 0.12, 0.0, 0.0);
        BaselineCharacteristics latency = characteristics(100.0, 4.0, 0.04, 0.1, 0.03, 0.0, 0.0);
        BaselineCharacteristics throughput = characteristics(1_000.0, 18.0, 0.018, 0.1, 0.01, 0.0, 0.0);
        var selector = new ru.eljke.driftguard.demo.research.CalibratedProfileSelector(List.of(
                new ru.eljke.driftguard.demo.research.CalibrationExample(
                        errorRate, DetectorSensitivityProfile.AGGRESSIVE
                ),
                new ru.eljke.driftguard.demo.research.CalibrationExample(
                        latency, DetectorSensitivityProfile.CONSERVATIVE
                ),
                new ru.eljke.driftguard.demo.research.CalibrationExample(
                        throughput, DetectorSensitivityProfile.CONSERVATIVE
                )
        ));

        assertEquals(
                DetectorSensitivityProfile.AGGRESSIVE,
                selector.select(characteristics(0.011, 0.0021, 0.19, 0.11, 0.11, 0.0, 0.0))
        );
        assertEquals(
                DetectorSensitivityProfile.CONSERVATIVE,
                selector.select(characteristics(950.0, 17.0, 0.018, 0.09, 0.01, 0.0, 0.0))
        );
    }

    @Test
    void experimentIsDeterministicApartFromCompletionTimestamp() {
        ResearchExperimentRequest request = new ResearchExperimentRequest(
                2, 120, 77L, List.of("latency-step"), List.of(1.0), List.of(1.0)
        );
        ResearchExperimentEngine engine = new ResearchExperimentEngine();

        ResearchExperimentReport first = engine.run(request, ignored -> { }, () -> false);
        ResearchExperimentReport repeated = engine.run(request, ignored -> { }, () -> false);

        assertEquals(first.trials(), repeated.trials());
        assertEquals(first.aggregates(), repeated.aggregates());
        assertFalse(first.trials().isEmpty());
    }

    private static BaselineCharacteristics characteristics(
            double mean,
            double standardDeviation,
            double coefficientOfVariation,
            double autocorrelation,
            double madRatio,
            double trend,
            double outlierRate
    ) {
        return new BaselineCharacteristics(
                mean,
                standardDeviation,
                coefficientOfVariation,
                autocorrelation,
                mean,
                madRatio,
                trend,
                outlierRate
        );
    }
}
