package ru.eljke.driftguard.demo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.demo.capability.DemoCapabilityGroup;
import ru.eljke.driftguard.demo.capability.DemoCapabilityService;
import ru.eljke.driftguard.demo.config.DemoConfigurationService;
import ru.eljke.driftguard.demo.config.DemoConfigurationView;
import ru.eljke.driftguard.demo.config.DemoToolProperties;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.demo.error.DemoErrorReason;
import ru.eljke.driftguard.demo.kafka.KafkaDemoService;
import ru.eljke.driftguard.demo.kafka.KafkaDemoStatus;
import ru.eljke.driftguard.demo.kafka.KafkaReplayRequest;
import ru.eljke.driftguard.demo.kafka.ops.KafkaOperationsService;
import ru.eljke.driftguard.demo.kafka.ops.KafkaOperationsSnapshot;
import ru.eljke.driftguard.demo.event.DemoDriftEventRepository;
import ru.eljke.driftguard.demo.event.DemoStoredDriftEvent;
import ru.eljke.driftguard.demo.scenario.DemoRunResult;
import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;
import ru.eljke.driftguard.demo.scenario.DemoScenarioDescriptor;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.demo.tool.ToolLink;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkReport;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "DriftGuard demo REST API")
@RequiredArgsConstructor
public class DemoController {
    private final DemoCapabilityService capabilityService;
    private final DemoScenarioService service;
    private final KafkaDemoService kafkaDemoService;
    private final KafkaOperationsService kafkaOperationsService;
    private final DemoToolProperties toolProperties;
    private final DemoConfigurationService configurationService;
    private final DemoDriftEventRepository eventRepository;

    @GetMapping
    @Operation(summary = "Return the latest synthetic scenario result")
    public DemoRunResult overview() {
        return service.lastResult();
    }

    @GetMapping("/events")
    @Operation(summary = "Return recent drift events")
    public List<DriftEvent> events() {
        return eventRepository.recentEvents(200);
    }

    @GetMapping("/events/stored")
    @Operation(summary = "Return stored drift events with source metadata")
    public List<DemoStoredDriftEvent> storedEvents() {
        return eventRepository.recent(200);
    }

    @PostMapping("/events/clear")
    @Operation(summary = "Clear stored drift events")
    public Map<String, Object> clearStoredEvents() {
        eventRepository.clear();
        return Map.of("cleared", true);
    }

    @GetMapping("/quality")
    @Operation(summary = "Return quality metrics for the latest run")
    public Object quality() {
        return service.lastResult().quality();
    }

    @GetMapping("/benchmark")
    @Operation(summary = "Run the detector benchmark for the active profile")
    public DetectionBenchmarkReport benchmark() {
        return service.benchmark();
    }

    @GetMapping("/benchmark/profiles")
    @Operation(summary = "Compare benchmark results across detector profiles")
    public List<DetectionBenchmarkReport> benchmarkProfiles() {
        return service.benchmarkProfiles();
    }

    @GetMapping("/scenarios")
    @Operation(summary = "List available synthetic scenarios")
    public List<DemoScenarioDescriptor> scenarios() {
        return service.scenarios();
    }

    @PostMapping("/run")
    @Operation(summary = "Run the default latency degradation scenario")
    public DemoRunResult rerun() {
        return service.runLatencyDegradation();
    }

    @PostMapping("/run/{scenario}")
    @Operation(summary = "Run a synthetic scenario immediately")
    public DemoRunResult runScenario(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) DemoScenarioRequest request
    ) {
        return service.run(scenario, request);
    }

    @PostMapping("/live/{scenario}")
    @Operation(summary = "Start live playback for a synthetic scenario")
    public DemoRunResult startLiveScenario(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) DemoScenarioRequest request
    ) {
        return service.startLive(scenario, request);
    }

    @PostMapping("/live/stop")
    @Operation(summary = "Stop live scenario playback")
    public DemoRunResult stopLiveScenario() {
        service.stopLive();
        return service.lastResult();
    }

    @GetMapping("/kafka")
    @Operation(summary = "Return Kafka demo playback status")
    public KafkaDemoStatus kafkaStatus() {
        return kafkaDemoService.status();
    }

    @GetMapping("/kafka/operations")
    @Operation(summary = "Return Kafka Streams processing metrics")
    public KafkaOperationsSnapshot kafkaOperations() {
        return kafkaOperationsService.snapshot();
    }

    @PostMapping("/kafka/start/{scenario}")
    @Operation(summary = "Start Kafka-backed scenario playback")
    public KafkaDemoStatus startKafkaScenario(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) DemoScenarioRequest request
    ) {
        return kafkaDemoService.start(scenario, request);
    }

    @PostMapping("/kafka/replay")
    @Operation(summary = "Replay a Kafka scenario with custom options")
    public KafkaDemoStatus replayKafkaScenario(@RequestBody(required = false) KafkaReplayRequest request) {
        return kafkaDemoService.replay(request);
    }

    @PostMapping("/kafka/stop")
    @Operation(summary = "Stop Kafka scenario playback")
    public KafkaDemoStatus stopKafkaScenario() {
        return kafkaDemoService.stop();
    }

    @GetMapping("/capabilities")
    @Operation(summary = "Return the demo capability map")
    public List<DemoCapabilityGroup> capabilities() {
        return capabilityService.capabilities();
    }

    @GetMapping("/tools")
    @Operation(summary = "Return links to local observability tools")
    public List<ToolLink> tools() {
        return List.of(
                new ToolLink("kafka-ui", "Kafka UI", toolProperties.getKafkaUiUrl(), "Inspect demo topics, produced metric points and drift events."),
                new ToolLink("prometheus", "Prometheus", toolProperties.getPrometheusUrl(), "Query DriftGuard and demo runtime metrics."),
                new ToolLink("grafana", "Grafana", toolProperties.getGrafanaUrl(), "Open dashboards for detector throughput, latency and emitted alerts."),
                new ToolLink("swagger", "Swagger", toolProperties.getSwaggerUrl(), "Explore the demo REST API.")
        );
    }

    @GetMapping("/configuration")
    @Operation(summary = "Return active detector and Kafka configuration")
    public DemoConfigurationView configuration() {
        return configurationService.current();
    }

    @PostMapping("/configuration/profile/{profile}")
    @Operation(summary = "Switch the active detector profile")
    public DemoConfigurationView updateProfile(@PathVariable("profile") String profile) {
        try {
            return configurationService.updateProfile(DemoDetectorProfile.parse(profile));
        } catch (IllegalArgumentException exception) {
            throw new DriftGuardValidationException(DemoErrorReason.UNKNOWN_PROFILE, profile);
        }
    }

    @GetMapping("/help")
    @Operation(summary = "Return available demo API routes")
    public Map<String, String> help() {
        return Map.ofEntries(
                Map.entry("serviceStatus", "GET /api/service"),
                Map.entry("serviceOperations", "GET /api/service/operations"),
                Map.entry("executeServiceOperation", "POST /api/service/operations"),
                Map.entry("startServiceTraffic", "POST /api/service/traffic/start"),
                Map.entry("stopServiceTraffic", "POST /api/service/traffic/stop"),
                Map.entry("resetServiceHistory", "POST /api/service/history/reset"),
                Map.entry("setServiceMode", "POST /api/service/mode/{mode}"),
                Map.entry("overview", "GET /api/demo"),
                Map.entry("events", "GET /api/demo/events"),
                Map.entry("quality", "GET /api/demo/quality"),
                Map.entry("storedEvents", "GET /api/demo/events/stored"),
                Map.entry("clearStoredEvents", "POST /api/demo/events/clear"),
                Map.entry("benchmarkProfiles", "GET /api/demo/benchmark/profiles"),
                Map.entry("scenarios", "GET /api/demo/scenarios"),
                Map.entry("rerun", "POST /api/demo/run"),
                Map.entry("runScenario", "POST /api/demo/run/{scenario}"),
                Map.entry("startLiveScenario", "POST /api/demo/live/{scenario}"),
                Map.entry("stopLiveScenario", "POST /api/demo/live/stop"),
                Map.entry("benchmark", "GET /api/demo/benchmark"),
                Map.entry("replayKafkaScenario", "POST /api/demo/kafka/replay"),
                Map.entry("kafkaStatus", "GET /api/demo/kafka"),
                Map.entry("kafkaOperations", "GET /api/demo/kafka/operations"),
                Map.entry("startKafkaScenario", "POST /api/demo/kafka/start/{scenario}"),
                Map.entry("stopKafkaScenario", "POST /api/demo/kafka/stop"),
                Map.entry("tools", "GET /api/demo/tools"),
                Map.entry("capabilities", "GET /api/demo/capabilities"),
                Map.entry("configuration", "GET /api/demo/configuration"),
                Map.entry("updateProfile", "POST /api/demo/configuration/profile/{profile}")
        );
    }
}


