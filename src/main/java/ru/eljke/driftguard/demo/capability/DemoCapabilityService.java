package ru.eljke.driftguard.demo.capability;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoCapabilityService {
    public List<DemoCapabilityGroup> capabilities() {
        return List.of(
                detectionEngine(),
                kafkaOperations(),
                qualityAndProfiles(),
                observabilityAndTooling()
        );
    }

    private DemoCapabilityGroup detectionEngine() {
        return new DemoCapabilityGroup(
                "detection-engine",
                "Detection engine",
                "Synthetic metric streams are processed by DriftGuard detectors and converted into alert events.",
                List.of(
                        ready(
                                "synthetic-scenarios",
                                "Synthetic scenarios",
                                "Run deterministic service degradation scenarios without external infrastructure.",
                                "engine",
                                List.of("GET /api/demo/scenarios", "POST /api/demo/run/{scenario}"),
                                List.of("Synthetic", "Overview")
                        ),
                        ready(
                                "live-playback",
                                "Live playback",
                                "Replay scenario points over time so the UI behaves like a running observability product.",
                                "engine",
                                List.of("POST /api/demo/live/{scenario}", "POST /api/demo/live/stop"),
                                List.of("Synthetic")
                        ),
                        partial(
                                "event-lifecycle",
                                "Event lifecycle",
                                "Inspect emitted alerts from instant, live and Kafka-backed scenario runs.",
                                "engine",
                                List.of("GET /api/demo/events", "GET /api/demo/events/stored"),
                                List.of("Kafka Demo", "Synthetic", "Overview")
                        ),
                        ready(
                                "stored-events",
                                "Stored drift events",
                                "Keep a bounded in-memory history of recent drift events for UI and API inspection.",
                                "engine",
                                List.of("GET /api/demo/events/stored", "POST /api/demo/events/clear"),
                                List.of("Overview")
                        )
                )
        );
    }

    private DemoCapabilityGroup kafkaOperations() {
        return new DemoCapabilityGroup(
                "kafka-operations",
                "Kafka operations",
                "Kafka producers, topics and Streams processing demonstrate library integration in a realistic pipeline.",
                List.of(
                        ready(
                                "kafka-scenario-replay",
                                "Kafka scenario replay",
                                "Publish scenario points into Kafka and let the DriftGuard topology emit alert events.",
                                "kafka",
                                List.of("POST /api/demo/kafka/start/{scenario}", "POST /api/demo/kafka/replay", "POST /api/demo/kafka/stop"),
                                List.of("Kafka Demo")
                        ),
                        partial(
                                "stateful-kafka-processing",
                                "Stateful Kafka processing",
                                "Show runtime status for the stateful Kafka Streams detector topology.",
                                "kafka",
                                List.of("GET /api/demo/kafka", "GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        ),
                        ready(
                                "kafka-operations-telemetry",
                                "Operations telemetry",
                                "Expose processing counters and latency measurements through Micrometer.",
                                "kafka",
                                List.of("GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        ),
                        planned(
                                "kafka-error-records",
                                "Kafka error records",
                                "Surface failed Kafka detection records when the topology is configured to route errors.",
                                "kafka",
                                List.of("GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        )
                )
        );
    }

    private DemoCapabilityGroup qualityAndProfiles() {
        return new DemoCapabilityGroup(
                "quality-and-profiles",
                "Quality and profiles",
                "Benchmark scenarios compare detector quality and profile sensitivity.",
                List.of(
                        ready(
                                "benchmark",
                                "Benchmark report",
                                "Measure precision, recall, missed intervals and detection delay for the active profile.",
                                "quality",
                                List.of("GET /api/demo/benchmark"),
                                List.of("Synthetic")
                        ),
                        ready(
                                "profile-comparison",
                                "Profile comparison",
                                "Compare aggressive, balanced and conservative threshold profiles on the same scenarios.",
                                "quality",
                                List.of("GET /api/demo/benchmark/profiles", "POST /api/demo/configuration/profile/{profile}"),
                                List.of("Synthetic", "Configuration")
                        ),
                        partial(
                                "quality-gates",
                                "Quality gates",
                                "Use testkit quality metrics to explain whether detector behavior is acceptable.",
                                "quality",
                                List.of("GET /api/demo/benchmark", "GET /api/demo/benchmark/profiles"),
                                List.of("Synthetic")
                        )
                )
        );
    }

    private DemoCapabilityGroup observabilityAndTooling() {
        return new DemoCapabilityGroup(
                "observability-and-tooling",
                "Observability and tooling",
                "Local tools make the demo inspectable beyond the main React UI.",
                List.of(
                        ready(
                                "configuration-view",
                                "Configuration view",
                                "Display active detector thresholds, emission policies and Kafka settings.",
                                "configuration",
                                List.of("GET /api/demo/configuration"),
                                List.of("Configuration")
                        ),
                        ready(
                                "tool-links",
                                "Tool links",
                                "Link directly to Kafka UI, Prometheus, Grafana and Swagger.",
                                "tools",
                                List.of("GET /api/demo/tools"),
                                List.of("Tools")
                        ),
                        ready(
                                "capability-map",
                                "Capability map",
                                "Show which product capabilities are ready, partial or planned.",
                                "tools",
                                List.of("GET /api/demo/capabilities"),
                                List.of("Overview")
                        )
                )
        );
    }

    private static DemoCapability ready(
            String id,
            String title,
            String description,
            String category,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return capability(id, title, description, category, DemoCapabilityStatus.READY, apiEndpoints, uiSurfaces);
    }

    private static DemoCapability partial(
            String id,
            String title,
            String description,
            String category,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return capability(id, title, description, category, DemoCapabilityStatus.PARTIAL, apiEndpoints, uiSurfaces);
    }

    @SuppressWarnings("SameParameterValue")
    private static DemoCapability planned(
            String id,
            String title,
            String description,
            String category,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return capability(id, title, description, category, DemoCapabilityStatus.PLANNED, apiEndpoints, uiSurfaces);
    }

    private static DemoCapability capability(
            String id,
            String title,
            String description,
            String category,
            DemoCapabilityStatus status,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return new DemoCapability(
                id,
                title,
                description,
                category,
                status,
                apiEndpoints,
                uiSurfaces
        );
    }
}


