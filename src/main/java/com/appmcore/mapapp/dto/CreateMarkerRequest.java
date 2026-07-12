package com.appmcore.mapapp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new marker symbol at a geographic location.
 * {@code label}, {@code color} and {@code size} are optional; the service
 * applies defaults when they are omitted.
 */
public record CreateMarkerRequest(

        @NotNull
        @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "latitude must be <= 90")
        BigDecimal latitude,

        @NotNull
        @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "longitude must be <= 180")
        BigDecimal longitude,

        @Size(max = 255, message = "label must be at most 255 characters")
        String label,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a #RRGGBB hex string")
        String color,

        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must be at most 100")
        Integer size) {
}
