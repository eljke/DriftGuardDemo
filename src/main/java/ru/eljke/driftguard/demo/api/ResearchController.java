package ru.eljke.driftguard.demo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eljke.driftguard.demo.research.ResearchExperimentRequest;
import ru.eljke.driftguard.demo.research.ResearchExperimentService;
import ru.eljke.driftguard.demo.research.ResearchJobSnapshot;

@RestController
@RequestMapping("/api/research")
@Tag(name = "Research", description = "Reproducible detector-profile experiments")
@RequiredArgsConstructor
public class ResearchController {
    private final ResearchExperimentService service;

    @PostMapping
    @Operation(summary = "Start a research experiment")
    public ResearchJobSnapshot start(@RequestBody(required = false) ResearchExperimentRequest request) {
        return service.start(request);
    }

    @GetMapping
    @Operation(summary = "Return current research experiment state")
    public ResearchJobSnapshot current() {
        return service.current();
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel the active research experiment")
    public ResearchJobSnapshot cancel() {
        return service.cancel();
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @Operation(summary = "Export the latest research report as CSV")
    public ResponseEntity<String> csv() {
        return download("driftguard-research.csv", "text/csv", service.csv());
    }

    @GetMapping(value = "/export.md", produces = "text/markdown")
    @Operation(summary = "Export the latest research report as Markdown")
    public ResponseEntity<String> markdown() {
        return download("driftguard-research.md", "text/markdown", service.markdown());
    }

    private static ResponseEntity<String> download(String fileName, String contentType, String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(body);
    }
}
