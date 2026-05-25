package ru.eljke.driftguard.demo.service;

/**
 * Request for executing one checkout operation.
 *
 * @param operation operation id; defaults to create-order
 * @param customerId optional customer id
 */
public record CheckoutOperationRequest(
        String operation,
        String customerId
) {
    public String normalizedOperation() {
        return operation == null || operation.isBlank() ? "create-order" : operation.trim();
    }

    public String normalizedCustomerId() {
        return customerId == null || customerId.isBlank() ? "anonymous" : customerId.trim();
    }
}
