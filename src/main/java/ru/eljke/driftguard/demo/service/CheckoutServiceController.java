package ru.eljke.driftguard.demo.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for the small checkout service used as the default demo product.
 */
@RestController
@RequestMapping("/api/service")
@Tag(name = "Checkout Service", description = "Operational service that emits metrics into DriftGuard")
@RequiredArgsConstructor
public class CheckoutServiceController {
    private final CheckoutService checkoutService;

    @GetMapping
    @Operation(summary = "Return checkout service status")
    public CheckoutServiceSnapshot status() {
        return checkoutService.snapshot();
    }

    @GetMapping("/operations")
    @Operation(summary = "List supported checkout operations")
    public List<String> operations() {
        return checkoutService.operations();
    }

    @PostMapping("/operations")
    @Operation(summary = "Execute one checkout operation")
    public CheckoutOperationResult execute(@RequestBody(required = false) CheckoutOperationRequest request) {
        return checkoutService.execute(request);
    }

    @PostMapping("/traffic/start")
    @Operation(summary = "Start automatic checkout traffic")
    public CheckoutServiceSnapshot startTraffic() {
        return checkoutService.startTraffic();
    }

    @PostMapping("/traffic/stop")
    @Operation(summary = "Stop automatic checkout traffic")
    public CheckoutServiceSnapshot stopTraffic() {
        return checkoutService.stopTraffic();
    }

    @PostMapping("/mode/{mode}")
    @Operation(summary = "Switch checkout service behavior mode")
    public CheckoutServiceSnapshot mode(@PathVariable("mode") CheckoutMode mode) {
        return checkoutService.setMode(mode);
    }
}
