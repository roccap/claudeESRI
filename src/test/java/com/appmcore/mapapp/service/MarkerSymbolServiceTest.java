package com.appmcore.mapapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appmcore.mapapp.dto.CreateMarkerRequest;
import com.appmcore.mapapp.dto.MarkerResponse;
import com.appmcore.mapapp.dto.UpdateMarkerLocationRequest;
import com.appmcore.mapapp.entity.MarkerSymbol;
import com.appmcore.mapapp.exception.MarkerNotFoundException;
import com.appmcore.mapapp.repository.MarkerSymbolRepository;

@ExtendWith(MockitoExtension.class)
class MarkerSymbolServiceTest {

    @Mock
    private MarkerSymbolRepository repository;

    @InjectMocks
    private MarkerSymbolService service;

    @Test
    void addMarkerPersistsRequestAndReturnsResponse() {
        CreateMarkerRequest request = new CreateMarkerRequest(
            new BigDecimal("51.5072"),
            new BigDecimal("-0.1276"),
            "London",
            "#00FF00",
            20);
        when(repository.save(any(MarkerSymbol.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarkerResponse response = service.addMarker(request);

        ArgumentCaptor<MarkerSymbol> captor = ArgumentCaptor.forClass(MarkerSymbol.class);
        verify(repository).save(captor.capture());
        MarkerSymbol persisted = captor.getValue();

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getLatitude()).isEqualByComparingTo("51.5072");
        assertThat(persisted.getLongitude()).isEqualByComparingTo("-0.1276");
        assertThat(persisted.getLabel()).isEqualTo("London");
        assertThat(persisted.getColor()).isEqualTo("#00FF00");
        assertThat(persisted.getSize()).isEqualTo(20);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(persisted.getId());
        assertThat(response.latitude()).isEqualByComparingTo("51.5072");
        assertThat(response.longitude()).isEqualByComparingTo("-0.1276");
        assertThat(response.label()).isEqualTo("London");
        assertThat(response.color()).isEqualTo("#00FF00");
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.createdAt()).isEqualTo(persisted.getCreatedAt());
    }

    @Test
    void addMarkerAppliesDefaultColorAndSizeWhenOmitted() {
        CreateMarkerRequest request = new CreateMarkerRequest(
            new BigDecimal("40.0"),
            new BigDecimal("-70.0"),
            null,
            null,
            null);
        when(repository.save(any(MarkerSymbol.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarkerResponse response = service.addMarker(request);

        assertThat(response.color()).isEqualTo("#E23131");
        assertThat(response.size()).isEqualTo(12);
        assertThat(response.label()).isNull();
    }

    @Test
    void listMarkersMapsAllStoredMarkers() {
        MarkerSymbol marker = MarkerSymbol.builder()
            .id(UUID.randomUUID())
            .latitude(new BigDecimal("51.5072"))
            .longitude(new BigDecimal("-0.1276"))
            .label("London")
            .color("#E23131")
            .size(12)
            .createdAt(Instant.parse("2026-07-10T00:00:00Z"))
            .build();
        when(repository.findAll()).thenReturn(List.of(marker));

        List<MarkerResponse> responses = service.listMarkers();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(marker.getId());
        assertThat(responses.getFirst().latitude()).isEqualByComparingTo("51.5072");
        assertThat(responses.getFirst().longitude()).isEqualByComparingTo("-0.1276");
        assertThat(responses.getFirst().label()).isEqualTo("London");
    }

    @Test
    void listMarkersReturnsEmptyListWhenNoneStored() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listMarkers()).isEmpty();
    }

    @Test
    void updateMarkerLocationMovesExistingMarker() {
        UUID id = UUID.randomUUID();
        MarkerSymbol existing = MarkerSymbol.builder()
            .id(id)
            .latitude(new BigDecimal("51.5072"))
            .longitude(new BigDecimal("-0.1276"))
            .label("London")
            .color("#E23131")
            .size(12)
            .createdAt(Instant.parse("2026-07-10T00:00:00Z"))
            .build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(MarkerSymbol.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMarkerLocationRequest request = new UpdateMarkerLocationRequest(
            new BigDecimal("48.8566"),
            new BigDecimal("2.3522"));

        MarkerResponse response = service.updateMarkerLocation(id, request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.latitude()).isEqualByComparingTo("48.8566");
        assertThat(response.longitude()).isEqualByComparingTo("2.3522");
        // Non-location attributes are preserved.
        assertThat(response.label()).isEqualTo("London");
        assertThat(response.color()).isEqualTo("#E23131");
        assertThat(response.size()).isEqualTo(12);
    }

    @Test
    void updateMarkerLocationThrowsWhenMarkerMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        UpdateMarkerLocationRequest request = new UpdateMarkerLocationRequest(
            new BigDecimal("48.8566"),
            new BigDecimal("2.3522"));

        assertThatThrownBy(() -> service.updateMarkerLocation(id, request))
            .isInstanceOf(MarkerNotFoundException.class)
            .hasMessageContaining(id.toString());
        verify(repository, never()).save(any(MarkerSymbol.class));
    }

    @Test
    void deleteMarkerRemovesExistingMarker() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        service.deleteMarker(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteMarkerThrowsWhenMarkerMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteMarker(id))
            .isInstanceOf(MarkerNotFoundException.class)
            .hasMessageContaining(id.toString());
        verify(repository, never()).deleteById(any(UUID.class));
    }
}
