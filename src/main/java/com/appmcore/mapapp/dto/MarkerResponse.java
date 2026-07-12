package com.appmcore.mapapp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

/**
 * Immutable representation of a persisted marker symbol returned to clients.
 */
@Builder
public record MarkerResponse(
        UUID id,
        BigDecimal latitude,
        BigDecimal longitude,
        String label,
        String color,
        int size,
        Instant createdAt) {
}
