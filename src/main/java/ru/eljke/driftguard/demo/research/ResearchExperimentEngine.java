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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
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
        List<CalibrationExample> calibrationExamples = new ArrayList<>();
        Map<String, Integer> bestProfileLabels = new LinkedHashMap<>();
        Map<DemoDetectorProfile, Double> calibrationUtility = new EnumMap<>(DemoDetectorProfile.class);
        Map<DemoDetectorProfile, Integer> calibrationUtilityCounts = new EnumMap<>(DemoDetectorProfile.class);
        int calibrationTrials = 0;
        int completed = 0;

        for (String scenarioId : request.scenarios()) {
            for (double noiseMultiplier : request.noiseMultipliers()) {
                for (double effectMultiplier : request.effectMultipliers()) {
                    for (int repetition = 0; repetition < request.calibrationRepetitions(); repetition++) {
                        if (cancelled.getAsBoolean()) {
                            throw new CancellationException("Research experiment was cancelled");
                        }
                        long seed = request.baseSeed() + repetition;
                        GeneratedScenario generated = generateScenario(
                                scenarioId,
                                noiseMultiplier,
                                effectMultiplier,
                                request.samples(),
                                seed
                        );
                        List<ResearchTrial> candidates = new ArrayList<>();
                        for (ResearchStrategy strategy : ResearchStrategy.fixed()) {
                            candidates.add(runTrial(
                                    scenarioId,
                                    generated.scenario(),
                                    generated.points(),
                                    strategy,
                                    strategy.fixedProfile(),
                                    seed,
                                    noiseMultiplier,
                                    effectMultiplier
                            ));
                            progress.accept(++completed);
                            calibrationTrials++;
                        }
                        ResearchTrial best = candidates.stream()
                                .max(java.util.Comparator.comparingDouble(
                                        trial -> utility(trial, request.samples())
                                ))
                                .orElseThrow();
                        for (ResearchTrial candidate : candidates) {
                            calibrationUtility.merge(
                                    candidate.selectedProfile(),
                                    utility(candidate, request.samples()),
                                    Double::sum
                            );
                            calibrationUtilityCounts.merge(candidate.selectedProfile(), 1, Integer::sum);
                        }
                        calibrationExamples.add(new CalibrationExample(
                                StreamCharacteristics.fromBaseline(generated.points()),
                                best.selectedProfile()
                        ));
                        bestProfileLabels.merge(best.selectedProfile().name(), 1, Integer::sum);
                    }
                }
            }
        }

        DemoDetectorProfile bestGlobalProfile = Arrays.stream(DemoDetectorProfile.values())
                .max(java.util.Comparator.comparingDouble(profile ->
                        calibrationUtility.get(profile) / calibrationUtilityCounts.get(profile)
                ))
                .orElseThrow();
        CalibratedProfileSelector selector = new CalibratedProfileSelector(calibrationExamples);
        List<ResearchTrial> trials = new ArrayList<>();
        for (String scenarioId : request.scenarios()) {
            for (double noiseMultiplier : request.noiseMultipliers()) {
                for (double effectMultiplier : request.effectMultipliers()) {
                    for (int repetition = 0; repetition < request.holdoutRepetitions(); repetition++) {
                        if (cancelled.getAsBoolean()) {
                            throw new CancellationException("Research experiment was cancelled");
                        }
                        long seed = request.baseSeed() + request.calibrationRepetitions() + repetition;
                        GeneratedScenario generated = generateScenario(
                                scenarioId,
                                noiseMultiplier,
                                effectMultiplier,
                                request.samples(),
                                seed
                        );
                        StreamCharacteristics characteristics = StreamCharacteristics.fromBaseline(generated.points());
                        for (ResearchStrategy strategy : ResearchStrategy.values()) {
                            DemoDetectorProfile profile = strategy == ResearchStrategy.ADAPTIVE
                                    ? selector.select(characteristics)
                                    : strategy.fixedProfile();
                            trials.add(runTrial(
                                    scenarioId,
                                    generated.scenario(),
                                    generated.points(),
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
                "A profile selector calibrated on robust baseline characteristics improves utility on unseen streams over fixed sensitivity profiles.",
                "Fixed profiles label calibration streams by utility. A standardized five-nearest-neighbor selector uses only baseline characteristics and is evaluated on disjoint deterministic hold-out seeds.",
                Instant.now(),
                request,
                trials.size(),
                new ResearchCalibrationSummary(
                        request.calibrationRepetitions(),
                        request.holdoutRepetitions(),
                        calibrationTrials,
                        trials.size(),
                        calibrationExamples.size(),
                        bestGlobalProfile,
                        Map.copyOf(bestProfileLabels)
                ),
                aggregate(trials),
                comparisons(trials, bestGlobalProfile, request.samples(), request.baseSeed()),
                List.copyOf(trials)
        );
    }

    private static GeneratedScenario generateScenario(
            String scenarioId,
            double noiseMultiplier,
            double effectMultiplier,
            int samples,
            long seed
    ) {
        MetricScenario scenario = DemoScenarioService.createScenario(
                scenarioId,
                "research-" + seed,
                scenarioRequest(scenarioId, samples, noiseMultiplier, effectMultiplier),
                seed
        );
        return new GeneratedScenario(scenario, scenario.generate());
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
        boolean driftExpected = !scenario.expectedDrifts().isEmpty();
        Double precision = driftExpected ? metrics.precision() : null;
        Double recall = driftExpected ? metrics.recall() : null;
        Double f1 = driftExpected ? f1(metrics.precision(), metrics.recall()) : null;
        Long delaySamples = metrics.firstDetectionDelay() == null
                ? null
                : metrics.firstDetectionDelay().toSeconds();
        Double specificity = driftExpected
                ? null
                : Math.max(0.0, 1.0 - metrics.falsePositiveEvents() / (double) points.size());
        Long timeToFirstFalseAlarm = driftExpected || events.isEmpty()
                ? null
                : Duration.between(points.getFirst().timestamp(), events.getFirst().detectedAt()).toSeconds();
        return new ResearchTrial(
                scenarioId,
                strategy,
                profile,
                seed,
                noiseMultiplier,
                effectMultiplier,
                driftExpected,
                precision,
                recall,
                f1,
                metrics.falsePositiveEvents(),
                metrics.falsePositiveEvents() * 1_000.0 / points.size(),
                delaySamples,
                specificity,
                metrics.falsePositiveEvents() == 0,
                timeToFirstFalseAlarm,
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
        Double meanF1 = meanNullable(trials, ResearchTrial::f1);
        double[] f1Values = values(trials, ResearchTrial::f1);
        double standardError = standardError(f1Values);
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
                meanNullable(trials, ResearchTrial::precision),
                meanNullable(trials, ResearchTrial::recall),
                meanF1,
                meanF1 == null ? null : Math.max(0.0, meanF1 - 1.96 * standardError),
                meanF1 == null ? null : Math.min(1.0, meanF1 + 1.96 * standardError),
                mean(trials, ResearchTrial::falsePositiveEventsPerThousand),
                meanNullable(trials, ResearchTrial::detectionDelaySamples),
                first.driftExpected() ? mean(trials, trial -> trial.detected() ? 1.0 : 0.0) : null,
                meanNullable(trials, ResearchTrial::specificity),
                mean(trials, trial -> trial.falseAlarmFree() ? 1.0 : 0.0),
                meanNullable(trials, ResearchTrial::timeToFirstFalseAlarmSamples),
                profiles
        );
    }

    private static double mean(List<ResearchTrial> trials, java.util.function.ToDoubleFunction<ResearchTrial> value) {
        return trials.stream().mapToDouble(value).average().orElse(0.0);
    }

    private static Double meanNullable(List<ResearchTrial> trials, Function<ResearchTrial, Number> value) {
        double[] values = values(trials, value);
        return values.length == 0 ? null : Arrays.stream(values).average().orElseThrow();
    }

    private static double[] values(List<ResearchTrial> trials, Function<ResearchTrial, Number> value) {
        return trials.stream()
                .map(value)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .toArray();
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

    static double utility(ResearchTrial trial, int samples) {
        double falsePositivePenalty = 0.25 * trial.falsePositiveEvents() / samples;
        if (!trial.driftExpected()) {
            return trial.specificity() - falsePositivePenalty;
        }
        double normalizedDelay = trial.detectionDelaySamples() == null
                ? 1.0
                : Math.min(1.0, trial.detectionDelaySamples() / (double) samples);
        return trial.f1() - falsePositivePenalty - 0.15 * normalizedDelay;
    }

    private static List<ResearchComparison> comparisons(
            List<ResearchTrial> trials,
            DemoDetectorProfile baselineProfile,
            int samples,
            long baseSeed
    ) {
        List<ResearchComparison> comparisons = new ArrayList<>();
        comparisons.add(comparison("ALL", trials, baselineProfile, samples, baseSeed));
        trials.stream()
                .map(ResearchTrial::scenario)
                .distinct()
                .forEach(scenario -> comparisons.add(comparison(
                        scenario,
                        trials.stream().filter(trial -> trial.scenario().equals(scenario)).toList(),
                        baselineProfile,
                        samples,
                        baseSeed + scenario.hashCode()
                )));
        return List.copyOf(comparisons);
    }

    private static ResearchComparison comparison(
            String scope,
            List<ResearchTrial> trials,
            DemoDetectorProfile baselineProfile,
            int samples,
            long bootstrapSeed
    ) {
        ResearchStrategy baselineStrategy = ResearchStrategy.valueOf(baselineProfile.name());
        Map<TrialKey, ResearchTrial> baseline = trials.stream()
                .filter(trial -> trial.strategy() == baselineStrategy)
                .collect(Collectors.toMap(TrialKey::from, Function.identity()));
        List<ResearchTrial> adaptive = trials.stream()
                .filter(trial -> trial.strategy() == ResearchStrategy.ADAPTIVE)
                .toList();
        double[] adaptiveUtility = adaptive.stream()
                .mapToDouble(trial -> utility(trial, samples))
                .toArray();
        double[] baselineUtility = adaptive.stream()
                .map(trial -> baseline.get(TrialKey.from(trial)))
                .mapToDouble(trial -> utility(trial, samples))
                .toArray();
        double[] differences = new double[adaptive.size()];
        for (int index = 0; index < differences.length; index++) {
            differences[index] = adaptiveUtility[index] - baselineUtility[index];
        }
        PairedStatistics.Result statistics = PairedStatistics.analyze(differences, bootstrapSeed);
        return new ResearchComparison(
                scope,
                baselineProfile,
                differences.length,
                Arrays.stream(adaptiveUtility).average().orElseThrow(),
                Arrays.stream(baselineUtility).average().orElseThrow(),
                statistics.mean(),
                statistics.confidenceLow(),
                statistics.confidenceHigh(),
                statistics.pValue(),
                statistics.wins(),
                statistics.losses(),
                statistics.ties()
        );
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

    private record GeneratedScenario(MetricScenario scenario, List<MetricPoint> points) {
    }

    private record TrialKey(String scenario, long seed, double noiseMultiplier, double effectMultiplier) {
        private static TrialKey from(ResearchTrial trial) {
            return new TrialKey(
                    trial.scenario(),
                    trial.seed(),
                    trial.noiseMultiplier(),
                    trial.effectMultiplier()
            );
        }
    }
}
