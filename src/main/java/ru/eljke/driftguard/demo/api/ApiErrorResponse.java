package ru.eljke.driftguard.demo.api;

import java.time.Instant;

/**
 * Error body returned by demo REST endpoints.
 *
 * @param timestamp response creation time
 * @param status HTTP status code
 * @param code stable application error code
 * @param message human-readable error message
 * @param path request path that failed
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}


