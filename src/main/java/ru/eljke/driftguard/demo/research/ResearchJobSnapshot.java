package ru.eljke.driftguard.demo.research;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ResearchJobSnapshot(
        String jobId,
        ResearchJobStatus status,
        int completedTrials,
        int totalTrials,
        Instant startedAt,
        Instant finishedAt,
        String error,
        ResearchExperimentReport report
) {
    @JsonProperty
    public double progressPercent() {
        return totalTrials == 0 ? 0.0 : completedTrials * 100.0 / totalTrials;
    }

    public static ResearchJobSnapshot idle() {
        return new ResearchJobSnapshot(
                null, ResearchJobStatus.IDLE, 0, 0, null, null, null, null
        );
    }
}
