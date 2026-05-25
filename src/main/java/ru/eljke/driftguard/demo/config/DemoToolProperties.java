package ru.eljke.driftguard.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Links to local tools displayed by the demo UI.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo.tools")
public class DemoToolProperties {
    /**
     * Kafka UI URL for inspecting demo topics.
     */
    private String kafkaUiUrl = "http://localhost:8090";

    /**
     * Prometheus URL for querying demo metrics.
     */
    private String prometheusUrl = "http://localhost:9090";

    /**
     * Grafana URL for detector dashboards.
     */
    private String grafanaUrl = "http://localhost:3000";

    /**
     * URL Swagger UI demo-application.
     */
    private String swaggerUrl = "/swagger-ui.html";
}


