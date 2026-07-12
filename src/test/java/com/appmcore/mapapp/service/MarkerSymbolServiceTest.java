package com.appmcore.mapapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appmcore.mapapp.dto.CreateMarkerRequest;
import com.appmcore.mapapp.dto.MarkerResponse;
import com.appmcore.mapapp.entity.MarkerSymbol;
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
}
