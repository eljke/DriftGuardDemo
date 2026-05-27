package ru.eljke.driftguard.demo.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo incident-router endpoint used by DriftGuard's built-in webhook alert
 * sink.
 *
 * <p>A real service would point {@code driftguard.alerts.webhook.url} to a
 * separate incident router, chat bot or automation endpoint. The demo keeps a
 * local receiver so the integration is runnable without external accounts.</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/alerts/driftguard")
public class DemoWebhookAlertController {
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Demo-Alert-Channel", required = false) String channel,
            @RequestBody DemoWebhookAlertPayload payload
    ) {
        log.warn(
                "Demo webhook alert received channel={} severity={} service={} metric={} operation={} title={}",
                channel,
                payload.severity(),
                payload.service(),
                payload.metric(),
                payload.operation(),
                payload.title()
        );
        return ResponseEntity.accepted().build();
    }
}
