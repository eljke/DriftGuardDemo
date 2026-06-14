package ru.eljke.driftguard.demo.research;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.demo.error.DemoErrorReason;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ResearchExperimentService {
    private final ResearchExperimentEngine engine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "driftguard-research");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<ResearchJobSnapshot> snapshot =
            new AtomicReference<>(ResearchJobSnapshot.idle());
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private volatile Future<?> task;

    public synchronized ResearchJobSnapshot start(ResearchExperimentRequest rawRequest) {
        ResearchJobSnapshot current = snapshot.get();
        if (current.status() == ResearchJobStatus.RUNNING) {
            throw new DriftGuardValidationException(DemoErrorReason.RESEARCH_JOB_RUNNING);
        }
        ResearchExperimentRequest request = rawRequest == null
                ? new ResearchExperimentRequest(null, null, null, null, null, null).normalized()
                : rawRequest.normalized();
        String jobId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        cancellationRequested.set(false);
        snapshot.set(new ResearchJobSnapshot(
                jobId,
                ResearchJobStatus.RUNNING,
                0,
                request.totalTrials(),
                startedAt,
                null,
                null,
                null
        ));
        task = executor.submit(() -> execute(jobId, startedAt, request));
        return snapshot.get();
    }

    public ResearchJobSnapshot current() {
        return snapshot.get();
    }

    public synchronized ResearchJobSnapshot cancel() {
        if (snapshot.get().status() == ResearchJobStatus.RUNNING) {
            cancellationRequested.set(true);
            Future<?> currentTask = task;
            if (currentTask != null) {
                currentTask.cancel(false);
            }
        }
        return snapshot.get();
    }

    public String csv() {
        return ResearchReportExporter.csv(requiredReport());
    }

    public String markdown() {
        return ResearchReportExporter.markdown(requiredReport());
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }

    private void execute(String jobId, Instant startedAt, ResearchExperimentRequest request) {
        try {
            ResearchExperimentReport report = engine.run(
                    request,
                    completed -> snapshot.updateAndGet(current -> new ResearchJobSnapshot(
                            jobId,
                            ResearchJobStatus.RUNNING,
                            completed,
                            current.totalTrials(),
                            startedAt,
                            null,
                            null,
                            null
                    )),
                    cancellationRequested::get
            );
            snapshot.set(new ResearchJobSnapshot(
                    jobId,
                    ResearchJobStatus.COMPLETED,
                    request.totalTrials(),
                    request.totalTrials(),
                    startedAt,
                    Instant.now(),
                    null,
                    report
            ));
        } catch (CancellationException exception) {
            ResearchJobSnapshot current = snapshot.get();
            snapshot.set(new ResearchJobSnapshot(
                    jobId,
                    ResearchJobStatus.CANCELLED,
                    current.completedTrials(),
                    current.totalTrials(),
                    startedAt,
                    Instant.now(),
                    null,
                    null
            ));
        } catch (RuntimeException exception) {
            ResearchJobSnapshot current = snapshot.get();
            snapshot.set(new ResearchJobSnapshot(
                    jobId,
                    ResearchJobStatus.FAILED,
                    current.completedTrials(),
                    current.totalTrials(),
                    startedAt,
                    Instant.now(),
                    exception.getMessage(),
                    null
            ));
        }
    }

    private ResearchExperimentReport requiredReport() {
        ResearchExperimentReport report = snapshot.get().report();
        if (report == null) {
            throw new DriftGuardValidationException(DemoErrorReason.RESEARCH_REPORT_UNAVAILABLE);
        }
        return report;
    }
}
