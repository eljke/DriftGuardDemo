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

        assertEquals(16, report.totalTrials());
        assertEquals(8, report.aggregates().size());
        assertTrue(report.aggregates().stream().allMatch(result -> result.trials() == 2));
        assertTrue(report.aggregates().stream().allMatch(result ->
                result.f1ConfidenceLow() <= result.meanF1()
                        && result.meanF1() <= result.f1ConfidenceHigh()
        ));
    }

    @Test
    void adaptiveSelectorUsesBaselineVariabilityAndSeasonality() {
        assertEquals(
                DemoDetectorProfile.AGGRESSIVE,
                ResearchStrategy.ADAPTIVE.profileFor(new StreamCharacteristics(100.0, 2.0, 0.02, 0.1))
        );
        assertEquals(
                DemoDetectorProfile.CONSERVATIVE,
                ResearchStrategy.ADAPTIVE.profileFor(new StreamCharacteristics(100.0, 5.0, 0.05, 0.8))
        );
        assertEquals(
                DemoDetectorProfile.CONSERVATIVE,
                ResearchStrategy.ADAPTIVE.profileFor(new StreamCharacteristics(1.0, 0.2, 0.2, 0.1))
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
}
