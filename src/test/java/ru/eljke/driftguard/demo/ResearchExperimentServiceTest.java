package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.demo.research.ResearchExperimentEngine;
import ru.eljke.driftguard.demo.research.ResearchExperimentRequest;
import ru.eljke.driftguard.demo.research.ResearchExperimentService;
import ru.eljke.driftguard.demo.research.ResearchJobSnapshot;
import ru.eljke.driftguard.demo.research.ResearchJobStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchExperimentServiceTest {
    @Test
    void completedJobCanBeExported() throws Exception {
        ResearchExperimentService service = new ResearchExperimentService(new ResearchExperimentEngine());
        try {
            service.start(new ResearchExperimentRequest(
                    2, 120, 90L, List.of("latency-step"), List.of(1.0), List.of(1.0)
            ));

            ResearchJobSnapshot result = awaitCompletion(service);

            assertEquals(ResearchJobStatus.COMPLETED, result.status());
            assertEquals(8, result.completedTrials());
            assertTrue(service.csv().contains("scenario,strategy,trials"));
            assertTrue(service.markdown().contains("# DriftGuard Research Experiment"));
        } finally {
            service.close();
        }
    }

    private static ResearchJobSnapshot awaitCompletion(ResearchExperimentService service) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            ResearchJobSnapshot snapshot = service.current();
            if (snapshot.status() != ResearchJobStatus.RUNNING) {
                return snapshot;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Research job did not finish");
    }
}
