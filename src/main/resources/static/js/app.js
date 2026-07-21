/**
 * ESRI ArcGIS map viewer.
 *
 * Fetches the initial view configuration from the backend
 * (/api/v1/map/config) and renders a map centred on London, UK.
 *
 * Interactions:
 *   - Click empty space          -> add a marker   (POST /api/v1/map/markers)
 *   - Click a marker             -> select it (highlighted)
 *   - Click the map while a
 *     marker is selected         -> move it there  (PUT .../{id}/location)
 *   - Right-click a marker       -> delete it      (DELETE .../{id})
 */
require(["esri/Map", "esri/views/MapView", "esri/Graphic"], function (Map, MapView, Graphic) {
    const loading = document.getElementById("loading");
    const toast = document.getElementById("toast");

    // Styling defaults mirror the backend's marker defaults.
    const DEFAULT_COLOR = "#E23131";
    const DEFAULT_SIZE = 12;

    let toastTimer = null;
    // The marker Graphic currently selected for moving, or null.
    let selectedGraphic = null;

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

        // Clicks drive select / move / add, so suppress the default popup.
        view.popupEnabled = false;

        // Drop a marker on the centre point. It has no server id, so it is a
        // static reference point and cannot be selected or moved.
        view.graphics.add(createMarkerGraphic({
            longitude: config.longitude,
            latitude: config.latitude,
            color: DEFAULT_COLOR,
            size: DEFAULT_SIZE,
            label: config.locationName || "Centre point"
        }));

        // Render any markers already persisted on the server.
        loadMarkers(view);

        view.on("click", (event) => {
            view.hitTest(event).then((response) => {
                const hit = response.results.find(
                    (result) => result.graphic
                        && result.graphic.attributes
                        && result.graphic.attributes.markerId
                );

                // Clicking an existing marker selects it.
                if (hit) {
                    selectMarker(hit.graphic);
                    return;
                }

                const point = event.mapPoint;
                if (!point || point.longitude == null || point.latitude == null) {
                    return;
                }

                // With a marker selected, the next map click moves it;
                // otherwise the click adds a new marker.
                if (selectedGraphic) {
                    moveSelectedMarker(point.longitude, point.latitude);
                } else {
                    addMarker(view, point.longitude, point.latitude);
                }
            });
        });

        // Right-clicking a marker deletes it. Suppress the browser context
        // menu over the map so the gesture is dedicated to deletion.
        view.container.addEventListener("contextmenu", (event) => event.preventDefault());
        view.on("pointer-down", (event) => {
            if (event.button !== 2) {
                return; // right mouse button only
            }
            view.hitTest(event).then((response) => {
                const hit = response.results.find(
                    (result) => result.graphic
                        && result.graphic.attributes
                        && result.graphic.attributes.markerId
                );
                if (hit) {
                    deleteMarker(view, hit.graphic);
                }
            });
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
     * Move the selected marker to a new location. Deselects immediately, then
     * updates the on-map position once the server confirms the change.
     */
    function moveSelectedMarker(longitude, latitude) {
        const graphic = selectedGraphic;
        const id = graphic.attributes.markerId;
        clearSelection();

        fetch("/api/v1/map/markers/" + id + "/location", {
            method: "PUT",
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
                graphic.geometry = {
                    type: "point",
                    longitude: marker.longitude,
                    latitude: marker.latitude
                };
                showToast("Marker moved");
            })
            .catch(() => showToast("Could not move marker", true));
    }

    /**
     * Delete a marker on the server, then remove its graphic from the map.
     * Clears the selection first if the deleted marker was selected.
     */
    function deleteMarker(view, graphic) {
        const id = graphic.attributes.markerId;
        if (selectedGraphic === graphic) {
            selectedGraphic = null;
        }

        fetch("/api/v1/map/markers/" + id, { method: "DELETE" })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }
                view.graphics.remove(graphic);
                showToast("Marker deleted");
            })
            .catch(() => showToast("Could not delete marker", true));
    }

    /** Highlight a marker as the current move target. */
    function selectMarker(graphic) {
        if (selectedGraphic === graphic) {
            return;
        }
        clearSelection();
        selectedGraphic = graphic;
        graphic.symbol = markerSymbol(graphic.attributes.color, graphic.attributes.size, true);
        showToast("Marker selected — click the map to move it");
    }

    /** Clear any current selection, restoring its normal styling. */
    function clearSelection() {
        if (selectedGraphic) {
            selectedGraphic.symbol = markerSymbol(
                selectedGraphic.attributes.color,
                selectedGraphic.attributes.size,
                false
            );
            selectedGraphic = null;
        }
    }

    /**
     * Build an ESRI point Graphic from a marker-shaped object
     * ({ id, longitude, latitude, color, size, label }). The server id (when
     * present) is stored in attributes so the graphic can be selected/moved.
     */
    function createMarkerGraphic(marker) {
        const color = marker.color || DEFAULT_COLOR;
        const size = marker.size || DEFAULT_SIZE;
        return new Graphic({
            geometry: {
                type: "point",
                longitude: marker.longitude,
                latitude: marker.latitude
            },
            symbol: markerSymbol(color, size, false),
            attributes: {
                markerId: marker.id || null,
                color: color,
                size: size
            },
            popupTemplate: {
                title: marker.label || "Marker"
            }
        });
    }

    /** A simple-marker symbol; when selected it gets a gold highlight outline. */
    function markerSymbol(color, size, selected) {
        return {
            type: "simple-marker",
            color: color || DEFAULT_COLOR,
            size: size || DEFAULT_SIZE,
            outline: selected
                ? { color: [255, 214, 0], width: 3 }
                : { color: [255, 255, 255], width: 1.5 }
        };
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
