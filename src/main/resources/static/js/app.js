/**
 * ESRI ArcGIS map viewer.
 *
 * Fetches the initial view configuration from the backend
 * (/api/v1/map/config) and renders a map centred on London, UK.
 * Clicking anywhere on the map persists a new marker via
 * POST /api/v1/map/markers and renders the marker returned by the server.
 */
require(["esri/Map", "esri/views/MapView", "esri/Graphic"], function (Map, MapView, Graphic) {
    const loading = document.getElementById("loading");
    const toast = document.getElementById("toast");

    // Styling defaults mirror the backend's marker defaults.
    const DEFAULT_COLOR = "#E23131";
    const DEFAULT_SIZE = 12;

    let toastTimer = null;

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
        view.graphics.add(createMarkerGraphic({
            longitude: config.longitude,
            latitude: config.latitude,
            color: DEFAULT_COLOR,
            size: DEFAULT_SIZE,
            label: config.locationName || "Centre point"
        }));

        // Render any markers already persisted on the server.
        loadMarkers(view);

        // Click anywhere to persist and render a new marker.
        view.on("click", (event) => {
            const point = event.mapPoint;
            if (!point || point.longitude == null || point.latitude == null) {
                return;
            }
            addMarker(view, point.longitude, point.latitude);
        });

        view.when(() => {
            if (loading) {
                loading.style.display = "none";
            }
        });
    }

    /**
     * Fetch previously persisted markers and render them. Failures are
     * non-fatal — the map still works, it just starts empty.
     */
    function loadMarkers(view) {
        fetch("/api/v1/map/markers")
            .then((response) => (response.ok ? response.json() : []))
            .catch(() => [])
            .then((markers) => {
                markers.forEach((marker) => view.graphics.add(createMarkerGraphic(marker)));
            });
    }

    /**
     * Persist a marker at the clicked location, then render the server's
     * response. Coordinates are rounded to 6 decimal places to match the
     * backend's stored precision.
     */
    function addMarker(view, longitude, latitude) {
        fetch("/api/v1/map/markers", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                longitude: Number(longitude.toFixed(6)),
                latitude: Number(latitude.toFixed(6))
            })
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }
                return response.json();
            })
            .then((marker) => {
                view.graphics.add(createMarkerGraphic(marker));
                showToast("Marker added");
            })
            .catch(() => showToast("Could not add marker", true));
    }

    /**
     * Build an ESRI point Graphic from a marker-shaped object
     * ({ longitude, latitude, color, size, label }).
     */
    function createMarkerGraphic(marker) {
        return new Graphic({
            geometry: {
                type: "point",
                longitude: marker.longitude,
                latitude: marker.latitude
            },
            symbol: {
                type: "simple-marker",
                color: marker.color || DEFAULT_COLOR,
                size: marker.size || DEFAULT_SIZE,
                outline: { color: [255, 255, 255], width: 1.5 }
            },
            popupTemplate: {
                title: marker.label || "Marker"
            }
        });
    }

    function showToast(message, isError) {
        if (!toast) {
            return;
        }
        toast.textContent = message;
        toast.classList.toggle("toast--error", Boolean(isError));
        toast.classList.add("toast--visible");

        if (toastTimer !== null) {
            clearTimeout(toastTimer);
        }
        toastTimer = setTimeout(() => {
            toast.classList.remove("toast--visible");
            toastTimer = null;
        }, 2500);
    }
});
