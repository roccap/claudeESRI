package com.appmcore.mapapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.appmcore.mapapp.dto.CreateMarkerRequest;
import com.appmcore.mapapp.dto.MarkerResponse;
import com.appmcore.mapapp.service.MarkerSymbolService;

@ExtendWith(MockitoExtension.class)
class MarkerControllerTest {

    @Mock
    private MarkerSymbolService markerSymbolService;

    @Test
    void addMarkerReturns201WithLocationAndBody() {
        MarkerController controller = new MarkerController(markerSymbolService);

        CreateMarkerRequest request = new CreateMarkerRequest(
            new BigDecimal("51.5072"),
            new BigDecimal("-0.1276"),
            "London",
            "#E23131",
            12);
        UUID id = UUID.randomUUID();
        MarkerResponse created = MarkerResponse.builder()
            .id(id)
            .latitude(request.latitude())
            .longitude(request.longitude())
            .label(request.label())
            .color(request.color())
            .size(request.size())
            .createdAt(Instant.parse("2026-07-10T00:00:00Z"))
            .build();
        when(markerSymbolService.addMarker(request)).thenReturn(created);

        ResponseEntity<MarkerResponse> response = controller.addMarker(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo("/api/v1/map/markers/" + id);
        assertThat(response.getBody()).isEqualTo(created);
    }
}
