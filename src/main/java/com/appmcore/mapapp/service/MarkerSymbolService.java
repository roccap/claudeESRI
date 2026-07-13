package com.appmcore.mapapp.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appmcore.mapapp.dto.CreateMarkerRequest;
import com.appmcore.mapapp.dto.MarkerResponse;
import com.appmcore.mapapp.dto.UpdateMarkerLocationRequest;
import com.appmcore.mapapp.entity.MarkerSymbol;
import com.appmcore.mapapp.exception.MarkerNotFoundException;
import com.appmcore.mapapp.repository.MarkerSymbolRepository;

/**
 * Business logic for creating and mapping map marker symbols.
 */
@Service
public class MarkerSymbolService {

    private static final Logger log = LoggerFactory.getLogger(MarkerSymbolService.class);

    /** Default marker colour — matches the client-side ESRI symbol (RGB 226,49,49). */
    private static final String DEFAULT_COLOR = "#E23131";
    private static final int DEFAULT_SIZE = 12;

    private final MarkerSymbolRepository repository;

    public MarkerSymbolService(MarkerSymbolRepository repository) {
        this.repository = repository;
    }

    /**
     * Persist a new marker symbol at the requested location, applying default
     * styling where the caller omitted it.
     *
     * @param request the validated creation request
     * @return the persisted marker as a response DTO (never {@code null})
     */
    @Transactional
    public MarkerResponse addMarker(CreateMarkerRequest request) {
        MarkerSymbol marker = MarkerSymbol.builder()
            .id(UUID.randomUUID())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .label(request.label())
            .color(request.color() != null ? request.color() : DEFAULT_COLOR)
            .size(request.size() != null ? request.size() : DEFAULT_SIZE)
            .createdAt(Instant.now())
            .build();

        MarkerSymbol saved = repository.save(marker);
        log.debug("Created marker {} at lat={} lon={}", saved.getId(), saved.getLatitude(), saved.getLongitude());

        return toResponse(saved);
    }

    /**
     * List all persisted marker symbols.
     *
     * @return every stored marker as a response DTO (never {@code null}; may be empty)
     */
    @Transactional(readOnly = true)
    public List<MarkerResponse> listMarkers() {
        return repository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Move an existing marker to a new latitude / longitude.
     *
     * @param id      the identifier of the marker to update
     * @param request the validated new location
     * @return the updated marker as a response DTO (never {@code null})
     * @throws MarkerNotFoundException if no marker exists for {@code id}
     */
    @Transactional
    public MarkerResponse updateMarkerLocation(UUID id, UpdateMarkerLocationRequest request) {
        MarkerSymbol marker = repository.findById(id)
            .orElseThrow(() -> new MarkerNotFoundException(id));

        marker.setLatitude(request.latitude());
        marker.setLongitude(request.longitude());

        MarkerSymbol saved = repository.save(marker);
        log.debug("Moved marker {} to lat={} lon={}", saved.getId(), saved.getLatitude(), saved.getLongitude());

        return toResponse(saved);
    }

    private MarkerResponse toResponse(MarkerSymbol marker) {
        return MarkerResponse.builder()
            .id(marker.getId())
            .latitude(marker.getLatitude())
            .longitude(marker.getLongitude())
            .label(marker.getLabel())
            .color(marker.getColor())
            .size(marker.getSize())
            .createdAt(marker.getCreatedAt())
            .build();
    }
}
