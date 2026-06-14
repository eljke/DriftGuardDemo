package ru.eljke.driftguard.demo.research;

import java.time.Instant;
import java.util.List;

public record ResearchExperimentReport(
        String hypothesis,
        String method,
        Instant completedAt,
        ResearchExperimentRequest request,
        int totalTrials,
        List<ResearchAggregate> aggregates,
        List<ResearchTrial> trials
) {
}
