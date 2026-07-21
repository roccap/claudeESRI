package com.appmcore.mapapp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.appmcore.mapapp.domain.MarkerShape;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A map marker rendered by the ESRI ArcGIS viewer at a geographic point.
 * The marker "symbol" is described by its colour and size; the point is
 * described by its latitude / longitude.
 */
@Entity
@Table(name = "marker_symbol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkerSymbol {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Latitude in decimal degrees, WGS84 (-90 .. 90). */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    /** Longitude in decimal degrees, WGS84 (-180 .. 180). */
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Optional human-readable label shown in the marker popup. */
    @Column(length = 255)
    private String label;

    /** Marker fill colour as a #RRGGBB hex string. */
    @Column(nullable = false, length = 7)
    private String color;

    /** Marker shape (ESRI simple-marker style). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarkerShape shape;

    /** Marker size in points. */
    @Column(nullable = false)
    private int size;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
