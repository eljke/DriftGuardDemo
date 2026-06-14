package ru.eljke.driftguard.demo.error;

import lombok.RequiredArgsConstructor;
import ru.eljke.driftguard.core.error.ErrorReason;

/**
 * Stable error codes returned by demo REST endpoints.
 */
@RequiredArgsConstructor
public enum DemoErrorReason implements ErrorReason {
    UNKNOWN_SCENARIO("DG-DEMO-001", "Unknown demo scenario: {}"),
    REQUEST_FAILED("DG-DEMO-002", "Demo request failed"),
    KAFKA_DEMO_DISABLED("DG-DEMO-003", "Kafka demo is disabled"),
    KAFKA_DEMO_FAILED("DG-DEMO-004", "Kafka demo failed: {}"),
    UNKNOWN_PROFILE("DG-DEMO-005", "Unknown detector profile: {}"),
    RESEARCH_JOB_RUNNING("DG-DEMO-006", "A research experiment is already running"),
    RESEARCH_REPORT_UNAVAILABLE("DG-DEMO-007", "Research report is not available");

    private final String code;
    private final String description;

    @Override
    public String code() {
        return code;
    }

    @Override
    public String description() {
        return description;
    }
}


