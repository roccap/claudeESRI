package com.appmcore.mapapp.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The shape of a marker symbol, mapping directly to an ESRI simple-marker
 * style. Serialised as its lower-case name (e.g. {@code "square"}) so the
 * front-end can pass it straight to the ArcGIS symbol.
 */
public enum MarkerShape {

    CIRCLE,
    SQUARE,
    DIAMOND,
    TRIANGLE,
    CROSS,
    X;

    /** Serialise as the lower-case ESRI style name. */
    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parse a shape case-insensitively.
     *
     * @throws IllegalArgumentException if {@code value} is not a known shape
     */
    @JsonCreator
    public static MarkerShape fromJson(String value) {
        return MarkerShape.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
