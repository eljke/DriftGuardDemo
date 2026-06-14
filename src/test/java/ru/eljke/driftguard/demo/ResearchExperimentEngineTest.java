package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.demo.research.ResearchExperimentEngine;
import ru.eljke.driftguard.demo.research.ResearchExperimentRequest;
import ru.eljke.driftguard.demo.research.ResearchExperimentReport;
import ru.eljke.driftguard.demo.research.ResearchStrategy;
import ru.eljke.driftguard.demo.research.StreamCharacteristics;

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
        StreamCharacteristics stable = characteristics(0.02, 0.1, 0.01, 0.0, 0.0);
        StreamCharacteristics seasonal = characteristics(0.15, 0.8, 0.12, 0.02, 0.05);
        var selector = new ru.eljke.driftguard.demo.research.CalibratedProfileSelector(List.of(
                new ru.eljke.driftguard.demo.research.CalibrationExample(
                        stable, DemoDetectorProfile.AGGRESSIVE
                ),
                new ru.eljke.driftguard.demo.research.CalibrationExample(
                        seasonal, DemoDetectorProfile.CONSERVATIVE
                )
        ));

        assertEquals(
                DemoDetectorProfile.AGGRESSIVE,
                selector.select(characteristics(0.021, 0.11, 0.011, 0.0, 0.0))
        );
        assertEquals(
                DemoDetectorProfile.CONSERVATIVE,
                selector.select(characteristics(0.14, 0.75, 0.11, 0.02, 0.04))
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

    private static StreamCharacteristics characteristics(
            double coefficientOfVariation,
            double autocorrelation,
            double madRatio,
            double trend,
            double outlierRate
    ) {
        return new StreamCharacteristics(
                100.0,
                coefficientOfVariation * 100.0,
                coefficientOfVariation,
                autocorrelation,
                100.0,
                madRatio,
                trend,
                outlierRate
        );
    }
}
