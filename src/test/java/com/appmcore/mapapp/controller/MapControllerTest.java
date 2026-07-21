package com.appmcore.mapapp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.appmcore.mapapp.dto.MapViewConfig;

class MapControllerTest {

    private final MapController controller = new MapController("test-api-key");

    @Test
    void initialViewIsCentredOnLondon() {
        MapViewConfig config = controller.initialView().getBody();

        assertThat(config).isNotNull();
        assertThat(config.longitude()).isEqualTo(-0.1276);
        assertThat(config.latitude()).isEqualTo(51.5072);
        assertThat(config.zoom()).isEqualTo(11);
        assertThat(config.locationName()).isEqualTo("London, UK");
        assertThat(config.basemap()).isEqualTo("arcgis/streets");
    }

    @Test
    void initialViewIncludesConfiguredApiKey() {
        MapViewConfig config = controller.initialView().getBody();

        assertThat(config).isNotNull();
        assertThat(config.apiKey()).isEqualTo("test-api-key");
    }
}
