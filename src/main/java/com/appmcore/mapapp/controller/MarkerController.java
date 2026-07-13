package com.appmcore.mapapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appmcore.mapapp.dto.CreateMarkerRequest;
import com.appmcore.mapapp.dto.MarkerResponse;
import com.appmcore.mapapp.service.MarkerSymbolService;

import jakarta.validation.Valid;

/**
 * REST endpoints for managing map marker symbols.
 */
@RestController
@RequestMapping("/api/v1/map/markers")
public class MarkerController {

    private final MarkerSymbolService markerSymbolService;

    public MarkerController(MarkerSymbolService markerSymbolService) {
        this.markerSymbolService = markerSymbolService;
    }

    /**
     * Create a new marker symbol at the supplied latitude / longitude.
     *
     * @return 201 Created with the persisted marker and a Location header
     */
    @PostMapping
    public ResponseEntity<MarkerResponse> addMarker(@Valid @RequestBody CreateMarkerRequest request) {
        MarkerResponse created = markerSymbolService.addMarker(request);
        URI location = URI.create("/api/v1/map/markers/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * List all persisted marker symbols.
     *
     * @return 200 OK with every stored marker (possibly empty)
     */
    @GetMapping
    public ResponseEntity<List<MarkerResponse>> listMarkers() {
        return ResponseEntity.ok(markerSymbolService.listMarkers());
    }
}
