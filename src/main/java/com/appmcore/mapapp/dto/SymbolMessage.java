package com.appmcore.mapapp.dto;

import java.math.BigDecimal;

/**
 * A live map symbol received on the Kafka stream and pushed to the browser.
 *
 * <p>Symbols are identified by {@code id}; a new id adds a symbol, a repeated id
 * updates the existing one in place. {@code shape} and {@code color} are passed
 * through to the front-end, which falls back to defaults for unknown values.
 */
public record SymbolMessage(
        String id,
        BigDecimal latitude,
        BigDecimal longitude,
        String shape,
        String color) {
}
