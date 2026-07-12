package com.appmcore.mapapp.exception;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * Standard error payload returned by {@link GlobalExceptionHandler}.
 */
@Builder
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details) {
}
