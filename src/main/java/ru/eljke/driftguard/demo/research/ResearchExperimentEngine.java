package ru.eljke.driftguard.demo.research;

import org.springframework.stereotype.Component;
import ru.eljke.driftguard.core.DriftGuard;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.testkit.benchmark.DetectionEvaluator;
import ru.eljke.driftguard.testkit.benchmark.DetectionMetrics;
import ru.eljke.driftguard.testkit.scenario.MetricScenario;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

@Component
public class ResearchExperimentEngine {
    public ResearchExperimentReport run(
            ResearchExperimentRequest rawRequest,
            IntConsumer progress,
            BooleanSupplier cancelled
    ) {
        ResearchExperimentRequest request = rawRequest.normalized();
        List<ResearchTrial> trials = new ArrayList<>(request.totalTrials());
        int completed = 0;

        for (String scenarioId : request.scenarios()) {
            for (double noiseMultiplier : request.noiseMultipliers()) {
                for (double effectMultiplier : request.effectMultipliers()) {
                    for (int repetition = 0; repetition < request.repetitions(); repetition++) {
                        if (cancelled.getAsBoolean()) {
                            throw new CancellationException("Research experiment was cancelled");
                        }
                        long seed = request.baseSeed() + repetition;
                        DemoScenarioRequest scenarioRequest = scenarioRequest(
                                scenarioId,
                                request.samples(),
                                noiseMultiplier,
                                effectMultiplier
                        );
                        MetricScenario scenario = DemoScenarioService.createScenario(
                                scenarioId,
                                "research-" + seed,
                                scenarioRequest,
                                seed
                        );
                        List<MetricPoint> points = scenario.generate();
                        StreamCharacteristics characteristics = StreamCharacteristics.fromBaseline(points);

                        for (ResearchStrategy strategy : ResearchStrategy.values()) {
                            DemoDetectorProfile profile = strategy.profileFor(characteristics);
                            trials.add(runTrial(
                                    scenarioId,
                                    scenario,
                                    points,
                                    strategy,
                                    profile,
                                    seed,
                                    noiseMultiplier,
                                    effectMultiplier
                            ));
                            progress.accept(++completed);
                        }
                    }
                }
            }
        }

        return new ResearchExperimentReport(
                "An adaptive profile selected from baseline stream characteristics improves the F1-delay trade-off over fixed sensitivity profiles.",
                "Paired deterministic trials use identical scenario parameters and seeds for every strategy. Means and normal-approximation 95% confidence intervals are reported.",
                Instant.now(),
                request,
                trials.size(),
                aggregate(trials),
                List.copyOf(trials)
        );
    }

    private static ResearchTrial runTrial(
            String scenarioId,
            MetricScenario scenario,
            List<MetricPoint> points,
            ResearchStrategy strategy,
            DemoDetectorProfile profile,
            long seed,
            double noiseMultiplier,
            double effectMultiplier
    ) {
        DriftGuard guard = DemoDetectionRuntime.createGuard(profile);
        List<DriftEvent> events = points.stream()
                .flatMap(point -> guard.detect(point).stream())
                .toList();
        DetectionMetrics metrics = DetectionEvaluator.evaluate(scenario, events);
        double f1 = f1(metrics.precision(), metrics.recall());
        long delaySamples = metrics.firstDetectionDelay() == null
                ? points.size()
                : metrics.firstDetectionDelay().toSeconds();
        return new ResearchTrial(
                scenarioId,
                strategy,
                profile,
                seed,
                noiseMultiplier,
                effectMultiplier,
                metrics.precision(),
                metrics.recall(),
                f1,
                metrics.falsePositiveEvents(),
                metrics.falsePositiveEvents() * 1_000.0 / points.size(),
                delaySamples,
                metrics.detected()
        );
    }

    private static List<ResearchAggregate> aggregate(List<ResearchTrial> trials) {
        Map<String, List<ResearchTrial>> groups = trials.stream()
                .collect(Collectors.groupingBy(
                        trial -> trial.scenario() + "|" + trial.strategy(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return groups.values().stream()
                .map(ResearchExperimentEngine::aggregateGroup)
                .toList();
    }

    private static ResearchAggregate aggregateGroup(List<ResearchTrial> trials) {
        ResearchTrial first = trials.getFirst();
        double meanF1 = mean(trials, ResearchTrial::f1);
        double standardError = standardError(trials.stream().mapToDouble(ResearchTrial::f1).toArray());
        Map<String, Integer> profiles = trials.stream()
                .collect(Collectors.toMap(
                        trial -> trial.selectedProfile().name(),
                        trial -> 1,
                        Integer::sum,
                        LinkedHashMap::new
                ));
        return new ResearchAggregate(
                first.scenario(),
                first.strategy(),
                trials.size(),
                mean(trials, ResearchTrial::precision),
                mean(trials, ResearchTrial::recall),
                meanF1,
                Math.max(0.0, meanF1 - 1.96 * standardError),
                Math.min(1.0, meanF1 + 1.96 * standardError),
                mean(trials, ResearchTrial::falsePositiveEventsPerThousand),
                mean(trials, trial -> trial.detectionDelaySamples()),
                mean(trials, trial -> trial.detected() ? 1.0 : 0.0),
                profiles
        );
    }

    private static double mean(List<ResearchTrial> trials, java.util.function.ToDoubleFunction<ResearchTrial> value) {
        return trials.stream().mapToDouble(value).average().orElse(0.0);
    }

    private static double standardError(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }
        double mean = java.util.Arrays.stream(values).average().orElse(0.0);
        double variance = java.util.Arrays.stream(values)
                .map(value -> Math.pow(value - mean, 2))
                .sum() / (values.length - 1);
        return Math.sqrt(variance / values.length);
    }

    private static double f1(double precision, double recall) {
        return precision + recall == 0.0 ? 0.0 : 2.0 * precision * recall / (precision + recall);
    }

    private static DemoScenarioRequest scenarioRequest(
            String scenarioId,
            int samples,
            double noiseMultiplier,
            double effectMultiplier
    ) {
        return switch (scenarioId) {
            case "latency-step" -> new DemoScenarioRequest(
                    samples, 100.0, 100.0 + 160.0 * effectMultiplier, 4.0 * noiseMultiplier, 50.0, null
            );
            case "error-rate-spike" -> new DemoScenarioRequest(
                    samples, 0.01, 0.01 + 0.17 * effectMultiplier, 0.002 * noiseMultiplier, 43.0, 20.0
            );
            case "throughput-drop" -> new DemoScenarioRequest(
                    samples, 1_000.0, 1_000.0 - 570.0 * effectMultiplier, 18.0 * noiseMultiplier, 47.0, null
            );
            case "queue-growth" -> new DemoScenarioRequest(
                    samples, 40.0, 2.6 * effectMultiplier, 4.0 * noiseMultiplier, 34.0, null
            );
            case "seasonal-latency" -> new DemoScenarioRequest(
                    samples, 120.0, 25.0 * effectMultiplier, 2.0 * noiseMultiplier, 50.0, null
            );
            default -> throw new IllegalArgumentException("Unsupported research scenario: " + scenarioId);
        };
    }
}
