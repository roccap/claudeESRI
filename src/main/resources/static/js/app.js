/**
 * ESRI ArcGIS map viewer.
 *
 * Fetches the initial view configuration from the backend
 * (/api/v1/map/config) and renders a map centred on London, UK.
 */
require(["esri/Map", "esri/views/MapView", "esri/Graphic"], function (Map, MapView, Graphic) {
    const loading = document.getElementById("loading");

    // Fallback view (London) in case the config endpoint is unreachable.
    const fallback = {
        longitude: -0.1276,
        latitude: 51.5072,
        zoom: 11,
        basemap: "arcgis/streets",
        locationName: "London, UK"
    };

    fetch("/api/v1/map/config")
        .then((response) => (response.ok ? response.json() : fallback))
        .catch(() => fallback)
        .then((config) => initMap(config));

    function initMap(config) {
        const map = new Map({
            basemap: config.basemap || "arcgis/streets"
        });

        const view = new MapView({
            container: "viewDiv",
            map: map,
            center: [config.longitude, config.latitude],
            zoom: config.zoom
        });

        // Drop a marker on the centre point.
        const marker = new Graphic({
            geometry: {
                type: "point",
                longitude: config.longitude,
                latitude: config.latitude
            },
            symbol: {
                type: "simple-marker",
                color: [226, 49, 49],
                outline: { color: [255, 255, 255], width: 1.5 }
            },
            popupTemplate: {
                title: config.locationName || "Centre point"
            }
        });
        view.graphics.add(marker);

        view.when(() => {
            if (loading) {
                loading.style.display = "none";
            }
        });
    }
});
