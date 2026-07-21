package com.appmcore.mapapp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.appmcore.mapapp.domain.MarkerShape;

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
        MarkerShape shape,
        Instant createdAt) {
}
