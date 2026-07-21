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
 *   - Right-click                -> context menu: switch the base map, and
 *                                   (over a marker) delete it (DELETE .../{id})
 */
require(["esri/config", "esri/Map", "esri/views/MapView", "esri/Graphic"], function (esriConfig, Map, MapView, Graphic) {
    const loading = document.getElementById("loading");
    const toast = document.getElementById("toast");

    // Styling defaults mirror the backend's marker defaults.
    const DEFAULT_COLOR = "#E23131";
    const DEFAULT_SIZE = 12;
    const DEFAULT_SHAPE = "circle";

    let toastTimer = null;
    // The marker Graphic currently selected for moving, or null.
    let selectedGraphic = null;

    // Base maps offered by the right-click context menu. The arcgis/* styles
    // need an ArcGIS API key to render; "osm" (OpenStreetMap) works without one.
    const BASEMAPS = [
        { id: "arcgis/streets", label: "Streets" },
        { id: "arcgis/imagery", label: "Imagery (Satellite)" },
        { id: "arcgis/topographic", label: "Topographic" },
        { id: "arcgis/navigation", label: "Navigation" },
        { id: "arcgis/dark-gray", label: "Dark Gray" },
        { id: "arcgis/light-gray", label: "Light Gray" },
        { id: "osm", label: "OpenStreetMap" }
    ];
    // The id of the currently active base map (set once the config loads).
    let currentBasemap = null;
    // The open context-menu element, or null.
    let contextMenuEl = null;

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
        // An ArcGIS API key (served from /api/v1/map/config) is required for the
        // arcgis/* basemaps to render; without one only key-free basemaps (OSM)
        // load and ArcGIS shows a sign-in prompt.
        if (config.apiKey) {
            esriConfig.apiKey = config.apiKey;
        }

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
            // Only the primary (left) button drives add/select/move; right-click
            // is reserved for the context menu, so ignore it here.
            if (event.button !== 0) {
                return;
            }
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

        // Track the active base map so the context menu can mark it.
        currentBasemap = config.basemap || "arcgis/streets";

        // Right-click opens a context menu: delete the clicked marker (if any)
        // and switch the base map. Suppress the browser menu over the map.
        view.container.addEventListener("contextmenu", (event) => {
            event.preventDefault();
            const rect = view.container.getBoundingClientRect();
            const screenPoint = { x: event.clientX - rect.left, y: event.clientY - rect.top };
            view.hitTest(screenPoint).then((response) => {
                const hit = response.results.find(
                    (result) => result.graphic
                        && result.graphic.attributes
                        && result.graphic.attributes.markerId
                );
                openContextMenu(view, map, event.clientX, event.clientY, hit ? hit.graphic : null);
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

    /**
     * Open the right-click context menu at (x, y) in viewport coordinates.
     * When a marker was right-clicked, a "Delete marker" item is shown above
     * the base-map choices; otherwise only the base-map choices appear.
     */
    function openContextMenu(view, map, x, y, markerGraphic) {
        closeContextMenu();

        const menu = document.createElement("div");
        menu.className = "context-menu";

        if (markerGraphic) {
            const del = document.createElement("button");
            del.type = "button";
            del.className = "context-menu__item context-menu__item--danger";
            del.textContent = "Delete marker";
            del.addEventListener("click", () => {
                closeContextMenu();
                deleteMarker(view, markerGraphic);
            });
            menu.appendChild(del);

            const divider = document.createElement("div");
            divider.className = "context-menu__divider";
            menu.appendChild(divider);
        }

        const header = document.createElement("div");
        header.className = "context-menu__header";
        header.textContent = "Base map";
        menu.appendChild(header);

        BASEMAPS.forEach((basemap) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "context-menu__item";
            if (basemap.id === currentBasemap) {
                item.classList.add("context-menu__item--active");
            }
            item.textContent = basemap.label;
            item.addEventListener("click", () => {
                closeContextMenu();
                if (basemap.id !== currentBasemap) {
                    map.basemap = basemap.id;
                    currentBasemap = basemap.id;
                    showToast("Base map: " + basemap.label);
                }
            });
            menu.appendChild(item);
        });

        // Place off-screen to measure, then clamp within the viewport.
        menu.style.left = "0";
        menu.style.top = "0";
        menu.style.visibility = "hidden";
        document.body.appendChild(menu);
        const rect = menu.getBoundingClientRect();
        menu.style.left = Math.max(4, Math.min(x, window.innerWidth - rect.width - 4)) + "px";
        menu.style.top = Math.max(4, Math.min(y, window.innerHeight - rect.height - 4)) + "px";
        menu.style.visibility = "visible";

        contextMenuEl = menu;
        // Defer so the initiating event doesn't immediately dismiss the menu.
        setTimeout(() => {
            document.addEventListener("pointerdown", onDocumentPointerDown, true);
            document.addEventListener("keydown", onDocumentKeyDown, true);
        }, 0);
    }

    /** Close the context menu if open and detach its dismissal listeners. */
    function closeContextMenu() {
        if (contextMenuEl) {
            contextMenuEl.remove();
            contextMenuEl = null;
            document.removeEventListener("pointerdown", onDocumentPointerDown, true);
            document.removeEventListener("keydown", onDocumentKeyDown, true);
        }
    }

    /** Dismiss the menu on any click outside it. */
    function onDocumentPointerDown(event) {
        if (contextMenuEl && !contextMenuEl.contains(event.target)) {
            closeContextMenu();
        }
    }

    /** Dismiss the menu on Escape. */
    function onDocumentKeyDown(event) {
        if (event.key === "Escape") {
            closeContextMenu();
        }
    }

    /** Highlight a marker as the current move target. */
    function selectMarker(graphic) {
        if (selectedGraphic === graphic) {
            return;
        }
        clearSelection();
        selectedGraphic = graphic;
        graphic.symbol = markerSymbol(
            graphic.attributes.color,
            graphic.attributes.size,
            true,
            graphic.attributes.shape
        );
        showToast("Marker selected — click the map to move it");
    }

    /** Clear any current selection, restoring its normal styling. */
    function clearSelection() {
        if (selectedGraphic) {
            selectedGraphic.symbol = markerSymbol(
                selectedGraphic.attributes.color,
                selectedGraphic.attributes.size,
                false,
                selectedGraphic.attributes.shape
            );
            selectedGraphic = null;
        }
    }

    /**
     * Build an ESRI point Graphic from a marker-shaped object
     * ({ id, longitude, latitude, color, size, shape, label }). The server id
     * (when present) is stored in attributes so the graphic can be
     * selected/moved, alongside the styling used to rebuild its symbol.
     */
    function createMarkerGraphic(marker) {
        const color = marker.color || DEFAULT_COLOR;
        const size = marker.size || DEFAULT_SIZE;
        const shape = marker.shape || DEFAULT_SHAPE;
        return new Graphic({
            geometry: {
                type: "point",
                longitude: marker.longitude,
                latitude: marker.latitude
            },
            symbol: markerSymbol(color, size, false, shape),
            attributes: {
                markerId: marker.id || null,
                color: color,
                size: size,
                shape: shape
            },
            popupTemplate: {
                title: marker.label || "Marker"
            }
        });
    }

    /**
     * A simple-marker symbol of the given shape (ESRI style); when selected it
     * gets a gold highlight outline.
     */
    function markerSymbol(color, size, selected, shape) {
        return {
            type: "simple-marker",
            style: shape || DEFAULT_SHAPE,
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
