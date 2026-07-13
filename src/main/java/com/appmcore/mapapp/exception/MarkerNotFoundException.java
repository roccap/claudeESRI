package com.appmcore.mapapp.exception;

import java.util.UUID;

/**
 * Thrown when a marker symbol cannot be found for the supplied identifier.
 */
public class MarkerNotFoundException extends RuntimeException {

    public MarkerNotFoundException(UUID id) {
        super("Marker not found: " + id);
    }
}
