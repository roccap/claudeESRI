package com.appmcore.mapapp.dto;

import lombok.Builder;

/**
 * Immutable initial map view configuration consumed by the ESRI ArcGIS
 * JavaScript viewer.
 */
@Builder
public record MapViewConfig(
        double longitude,
        double latitude,
        int zoom,
        String basemap,
        String locationName) {
}
