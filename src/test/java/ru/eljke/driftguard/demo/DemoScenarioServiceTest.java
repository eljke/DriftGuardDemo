package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftEventPhase;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.demo.event.InMemoryDemoDriftEventRepository;
import ru.eljke.driftguard.demo.scenario.DemoRunResult;
import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.testkit.scenario.MetricScenario;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoScenarioServiceTest {
    @Test
    void runLatencyDegradationProducesEvents() {
        DemoRunResult result = service().runLatencyDegradation();

        assertFalse(result.events().isEmpty());
        assertTrue(result.quality().detected());
        assertTrue(result.events().size() <= 1);
    }

    @Test
    void errorRateSpikeProducesRecoveryAfterReturningToBaseline() {
        DemoDetectionRuntime runtime = new DemoDetectionRuntime();
        MetricScenario scenario = DemoScenarioService.createScenario("error-rate-spike", "test-error-rate-spike");
        List<DriftEvent> events = new ArrayList<>();

        for (MetricPoint point : scenario.generate()) {
            events.addAll(runtime.detect(point));
        }

        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(event -> event.key().metric().equals("error-rate")));
        assertTrue(events.stream().anyMatch(event -> event.phase() == DriftEventPhase.RECOVERED));
    }

    @Test
    void runScenarioUsesRequestedSampleCount() {
        DemoScenarioService service = service();

        DemoRunResult result = service.run("latency-step", new DemoScenarioRequest(240));

        assertEquals(240, result.metricPoints());
        assertEquals(240, result.samplePoints().size());
    }

    @Test
    void scenarioSeedMakesGeneratedStreamReproducible() {
        DemoScenarioRequest request = new DemoScenarioRequest(120);

        List<Double> first = DemoScenarioService.createScenario("latency-step", "seed-a", request, 17L)
                .generate()
                .stream()
                .map(MetricPoint::value)
                .toList();
        List<Double> repeated = DemoScenarioService.createScenario("latency-step", "seed-b", request, 17L)
                .generate()
                .stream()
                .map(MetricPoint::value)
                .toList();
        List<Double> different = DemoScenarioService.createScenario("latency-step", "seed-c", request, 18L)
                .generate()
                .stream()
                .map(MetricPoint::value)
                .toList();

        assertEquals(first, repeated);
        assertFalse(first.equals(different));
    }

    @Test
    void independentProfileGuardDetectsWithoutChangingActiveRuntime() {
        DemoDetectionRuntime runtime = new DemoDetectionRuntime();
        var guard = DemoDetectionRuntime.createGuard(DemoDetectorProfile.AGGRESSIVE);
        MetricScenario scenario = DemoScenarioService.createScenario("latency-step", "independent-runtime");

        List<DriftEvent> events = scenario.generate().stream()
                .flatMap(point -> guard.detect(point).stream())
                .toList();

        assertFalse(events.isEmpty());
        assertEquals(DemoDetectorProfile.BALANCED, runtime.profile());
    }

    @Test
    void adaptiveProfileReportsSelectedLibraryProfile() {
        var guard = DemoDetectionRuntime.createGuard(DemoDetectorProfile.ADAPTIVE);
        MetricScenario scenario = DemoScenarioService.createScenario(
                "error-rate-spike",
                "adaptive-runtime",
                new DemoScenarioRequest(500),
                42L
        );

        List<DriftEvent> events = scenario.generate().stream()
                .flatMap(point -> guard.detect(point).stream())
                .toList();

        assertFalse(events.isEmpty());
        assertEquals("ADAPTIVE", events.getFirst().details().get("profileSelection"));
        assertEquals("AGGRESSIVE", events.getFirst().details().get("selectedProfile"));
    }

    private static DemoScenarioService service() {
        return new DemoScenarioService(
                new DemoDetectionRuntime(),
                new SimpleMeterRegistry(),
                new InMemoryDemoDriftEventRepository()
        );
    }
}
