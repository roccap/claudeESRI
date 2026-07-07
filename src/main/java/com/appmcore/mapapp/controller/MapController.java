package com.appmcore.mapapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appmcore.mapapp.dto.MapViewConfig;

/**
 * Exposes the initial map view configuration to the browser front-end.
 * The ESRI ArcGIS viewer fetches this on load to centre the map.
 */
@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    // Initial view: centred on London, UK.
    private static final double LONDON_LONGITUDE = -0.1276;
    private static final double LONDON_LATITUDE = 51.5072;
    private static final int DEFAULT_ZOOM = 11;

    @GetMapping("/config")
    public ResponseEntity<MapViewConfig> initialView() {
        MapViewConfig config = MapViewConfig.builder()
            .longitude(LONDON_LONGITUDE)
            .latitude(LONDON_LATITUDE)
            .zoom(DEFAULT_ZOOM)
            .basemap("arcgis/streets")
            .locationName("London, UK")
            .build();
        return ResponseEntity.ok(config);
    }
}
