package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiEndpointTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DriftGuard Demo API"))
                .andExpect(jsonPath("$.paths['/api/demo/run']").exists())
                .andExpect(jsonPath("$.paths['/api/demo/kafka/start/{scenario}']").exists())
                .andExpect(jsonPath("$.paths['/api/demo/kafka/operations']").exists())
                .andExpect(jsonPath("$.paths['/api/demo/tools']").exists())
                .andExpect(jsonPath("$.components.schemas.DriftEvent.description").exists())
                .andExpect(jsonPath("$.components.schemas.MetricPoint.properties.value.description").exists())
                .andExpect(jsonPath("$.components.schemas.DemoRunResult.properties.quality.description").exists())
                .andExpect(jsonPath("$.components.schemas.KafkaDemoStatus.properties.inputTopic.description").exists())
                .andExpect(jsonPath("$.components.schemas.KafkaOperationsSnapshot").exists())
                .andExpect(jsonPath("$.components.schemas.KafkaProducerStatus.properties.service.description").exists())
                .andExpect(jsonPath("$.components.schemas.DemoConfigurationView.properties.aggressiveness.description").exists())
                .andExpect(jsonPath("$.components.schemas.DetectorConfigurationView.properties.sensitivity.description").exists())
                .andExpect(jsonPath("$.components.schemas.ToolLink.properties.url.description").exists());
    }

    @Test
    void switchesCheckoutServiceMode() throws Exception {
        mockMvc.perform(post("/api/service/mode/DEGRADED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("DEGRADED"));
    }

    @Test
    void resetsCheckoutServiceHistory() throws Exception {
        mockMvc.perform(post("/api/service/history/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operations").value(0))
                .andExpect(jsonPath("$.recentMetrics").isEmpty());
    }

    @Test
    void acceptsBuiltInWebhookAlertPayload() throws Exception {
        mockMvc.perform(post("/internal/alerts/driftguard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Demo-Alert-Channel", "test")
                        .content("""
                                {
                                  "id": "alert-1",
                                  "severity": "CRITICAL",
                                  "title": "Latency drift",
                                  "message": "latency increased",
                                  "service": "checkout-service",
                                  "metric": "latency",
                                  "operation": "POST /checkout",
                                  "labels": {"detector": "latency-page-hinkley"}
                                }
                                """))
                .andExpect(status().isAccepted());
    }
}
