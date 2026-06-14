package ru.eljke.driftguard.demo.detection;

import org.springframework.stereotype.Service;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.adaptive.AdaptivePageHinkleyConfig;
import ru.eljke.driftguard.algorithms.adaptive.PageHinkleyProfileSelector;
import ru.eljke.driftguard.algorithms.adaptive.ScaleAwareProfileSelector;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.DriftGuard;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.config.MetricSelector;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the active DriftGuard facade and detector definitions for demo scenarios.
 *
 * <p>The runtime can switch between predefined detector profiles without
 * restarting the application.</p>
 */
@Service
public class DemoDetectionRuntime {
    private final AtomicLong versionSequence = new AtomicLong(1);
    private final AtomicReference<RuntimeState> state = new AtomicReference<>(create(DemoDetectorProfile.BALANCED, 1));

    public List<DriftEvent> detect(MetricPoint point) {
        return state.get().guard().detect(point);
    }

    public DemoDetectorProfile profile() {
        return state.get().profile();
    }

    public long version() {
        return state.get().version();
    }

    public List<DetectorDefinition> definitions() {
        return state.get().definitions();
    }

    public synchronized void reset() {
        state.set(create(profile(), versionSequence.incrementAndGet()));
    }

    public synchronized DemoDetectorProfile setProfile(DemoDetectorProfile profile) {
        state.set(create(profile, versionSequence.incrementAndGet()));
        return profile;
    }

    public static DriftGuard createGuard(DemoDetectorProfile profile) {
        return create(profile, 0).guard();
    }

    public static DriftGuard createAdaptiveGuard(
            PageHinkleyProfileSelector selector,
            int calibrationSamples
    ) {
        List<DetectorDefinition> definitions = adaptiveDefinitions(selector, calibrationSamples);
        return DriftGuard.builder()
                .registry(DefaultAlgorithms.registry())
                .definitions(definitions)
                .build();
    }

    private static RuntimeState create(DemoDetectorProfile profile, long version) {
        List<DetectorDefinition> definitions = definitions(profile);
        return new RuntimeState(
                profile,
                version,
                definitions,
                DriftGuard.builder()
                        .registry(DefaultAlgorithms.registry())
                        .definitions(definitions)
                        .build()
        );
    }

    private static List<DetectorDefinition> definitions(DemoDetectorProfile profile) {
        if (profile == DemoDetectorProfile.ADAPTIVE) {
            return adaptiveDefinitions(new ScaleAwareProfileSelector(), 100);
        }
        ProfileSettings settings = ProfileSettings.of(profile);
        return List.of(
                pageHinkley(
                        "latency-page-hinkley",
                        "latency",
                        settings.latencyWarning(),
                        settings.latencyCritical(),
                        0.1,
                        DriftDirection.UP,
                        settings
                ),
                pageHinkley(
                        "error-rate-page-hinkley",
                        "error-rate",
                        settings.errorRateWarning(),
                        settings.errorRateCritical(),
                        0.001,
                        DriftDirection.UP,
                        settings
                ),
                pageHinkley(
                        "queue-size-page-hinkley",
                        "queue-size",
                        settings.queueWarning(),
                        settings.queueCritical(),
                        0.1,
                        DriftDirection.UP,
                        settings
                ),
                pageHinkley(
                        "throughput-page-hinkley",
                        "throughput",
                        settings.throughputWarning(),
                        settings.throughputCritical(),
                        1.0,
                        DriftDirection.DOWN,
                        settings
                )
        );
    }

    private static List<DetectorDefinition> adaptiveDefinitions(
            PageHinkleyProfileSelector selector,
            int calibrationSamples
    ) {
        ProfileSettings aggressive = ProfileSettings.of(DemoDetectorProfile.AGGRESSIVE);
        ProfileSettings balanced = ProfileSettings.of(DemoDetectorProfile.BALANCED);
        ProfileSettings conservative = ProfileSettings.of(DemoDetectorProfile.CONSERVATIVE);
        return List.of(
                adaptivePageHinkley(
                        "latency-adaptive-page-hinkley",
                        "latency",
                        0.1,
                        DriftDirection.UP,
                        selector,
                        calibrationSamples,
                        aggressive,
                        balanced,
                        conservative
                ),
                adaptivePageHinkley(
                        "error-rate-adaptive-page-hinkley",
                        "error-rate",
                        0.001,
                        DriftDirection.UP,
                        selector,
                        calibrationSamples,
                        aggressive,
                        balanced,
                        conservative
                ),
                adaptivePageHinkley(
                        "queue-size-adaptive-page-hinkley",
                        "queue-size",
                        0.1,
                        DriftDirection.UP,
                        selector,
                        calibrationSamples,
                        aggressive,
                        balanced,
                        conservative
                ),
                adaptivePageHinkley(
                        "throughput-adaptive-page-hinkley",
                        "throughput",
                        1.0,
                        DriftDirection.DOWN,
                        selector,
                        calibrationSamples,
                        aggressive,
                        balanced,
                        conservative
                )
        );
    }

    private static DetectorDefinition adaptivePageHinkley(
            String name,
            String metric,
            double delta,
            DriftDirection direction,
            PageHinkleyProfileSelector selector,
            int calibrationSamples,
            ProfileSettings aggressive,
            ProfileSettings balanced,
            ProfileSettings conservative
    ) {
        AdaptivePageHinkleyConfig config = new AdaptivePageHinkleyConfig(
                calibrationSamples,
                selector,
                pageHinkleyConfig(metric, delta, direction, aggressive),
                pageHinkleyConfig(metric, delta, direction, balanced),
                pageHinkleyConfig(metric, delta, direction, conservative),
                aggressive.emissionPolicy(),
                balanced.emissionPolicy(),
                conservative.emissionPolicy()
        );
        return DetectorDefinition.builder()
                .name(name)
                .config(config)
                .appliesTo(MetricSelector.builder().metric(metric).build())
                .emissionPolicy(balanced.emissionPolicy())
                .build();
    }

    private static DetectorDefinition pageHinkley(
            String name,
            String metric,
            double warningThreshold,
            double criticalThreshold,
            double delta,
            DriftDirection direction,
            ProfileSettings settings
    ) {
        return DetectorDefinition.builder()
                .name(name)
                .config(pageHinkleyConfig(
                        settings.warmupSamples(),
                        delta,
                        warningThreshold,
                        criticalThreshold,
                        direction
                ))
                .appliesTo(MetricSelector.builder()
                        .metric(metric)
                        .build())
                .emissionPolicy(settings.emissionPolicy())
                .build();
    }

    private static PageHinkleyConfig pageHinkleyConfig(
            String metric,
            double delta,
            DriftDirection direction,
            ProfileSettings settings
    ) {
        return switch (metric) {
            case "latency" -> pageHinkleyConfig(
                    settings.warmupSamples(), delta, settings.latencyWarning(), settings.latencyCritical(), direction
            );
            case "error-rate" -> pageHinkleyConfig(
                    settings.warmupSamples(), delta, settings.errorRateWarning(), settings.errorRateCritical(), direction
            );
            case "queue-size" -> pageHinkleyConfig(
                    settings.warmupSamples(), delta, settings.queueWarning(), settings.queueCritical(), direction
            );
            case "throughput" -> pageHinkleyConfig(
                    settings.warmupSamples(), delta, settings.throughputWarning(), settings.throughputCritical(), direction
            );
            default -> throw new IllegalArgumentException("Unsupported adaptive metric: " + metric);
        };
    }

    private static PageHinkleyConfig pageHinkleyConfig(
            int warmupSamples,
            double delta,
            double warningThreshold,
            double criticalThreshold,
            DriftDirection direction
    ) {
        return PageHinkleyConfig.builder()
                .warmupSamples(warmupSamples)
                .delta(delta)
                .warningThreshold(warningThreshold)
                .criticalThreshold(criticalThreshold)
                .alpha(0.05)
                .direction(direction)
                .build();
    }

    private record RuntimeState(
            DemoDetectorProfile profile,
            long version,
            List<DetectorDefinition> definitions,
            DriftGuard guard
    ) {
    }

        private record ProfileSettings(
            double latencyWarning,
            double latencyCritical,
            double errorRateWarning,
            double errorRateCritical,
            double queueWarning,
            double queueCritical,
            double throughputWarning,
            double throughputCritical,
            int warmupSamples,
            EmissionPolicyConfig emissionPolicy
    ) {
        private static ProfileSettings of(DemoDetectorProfile profile) {
            return switch (profile) {
                case AGGRESSIVE -> new ProfileSettings(
                        25.0, 80.0,
                        0.025, 0.09,
                        25.0, 70.0,
                        90.0, 180.0,
                        6,
                        emissionPolicy(1, Duration.ofSeconds(8))
                );
                case BALANCED -> new ProfileSettings(
                        35.0, 115.0,
                        0.045, 0.14,
                        35.0, 110.0,
                        120.0, 250.0,
                        8,
                        emissionPolicy(1, Duration.ofSeconds(12))
                );
                case CONSERVATIVE -> new ProfileSettings(
                        70.0, 190.0,
                        0.07, 0.20,
                        75.0, 180.0,
                        180.0, 350.0,
                        14,
                        emissionPolicy(2, Duration.ofSeconds(25))
                );
                case ADAPTIVE -> throw new IllegalArgumentException("Adaptive profile has per-stream settings");
            };
        }

        private static EmissionPolicyConfig emissionPolicy(int minConsecutiveSignals, Duration cooldown) {
            return EmissionPolicyConfig.builder()
                    .minConsecutiveSignals(minConsecutiveSignals)
                    .cooldown(cooldown)
                    .recoveryConsecutiveNormal(1)
                    .build();
        }
    }
}


