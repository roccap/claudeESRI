# Map Application

A Spring Boot web application that renders an interactive map using
**ESRI ArcGIS web tools** (the ArcGIS Maps SDK for JavaScript). The initial
view is centred on **London, UK**.

## Tech Stack

| | |
|---|---|
| Language   | Java 21 |
| Framework  | Spring Boot 4.0.5 |
| Build      | Maven |
| Mapping    | ESRI ArcGIS Maps SDK for JavaScript (4.31, via CDN) |
| Database   | H2 (dev) / PostgreSQL (staging & prod) |
| Group / Artifact | `com.appmcore` / `map-app` |

## Features

- Interactive ArcGIS map centred on London (`-0.1276, 51.5072`).
- Initial view configuration served from a versioned REST endpoint
  (`/api/v1/map/config`) so the map centre, zoom and basemap can be changed
  server-side.
- Marker symbols persisted via a versioned REST API — create at a latitude /
  longitude, list, and move an existing marker to a new location (JPA-backed;
  H2 in dev).
- Click-to-add and click-to-move marker interactions in the front-end: click
  empty space to drop a marker, click a marker to select it, then click the
  map to move it.
- Central JSON error handling via `@RestControllerAdvice` (validation → `400`,
  unknown marker → `404`).
- Spring Security with sensible public defaults (map page + health endpoint).
- Spring Boot Actuator for health / metrics.
- Spring Retry + AOP enabled for resilient service calls.
- Profile-based configuration for dev, staging and prod.

## Project Layout

```
src/main/java/com/appmcore/mapapp
├── MapAppApplication.java              # entry point (@EnableRetry)
├── config/SecurityConfig.java          # web security rules
├── controller/
│   ├── MapController.java              # /api/v1/map/config
│   └── MarkerController.java           # /api/v1/map/markers
├── service/MarkerSymbolService.java    # marker business logic
├── repository/MarkerSymbolRepository.java
├── entity/MarkerSymbol.java            # persisted marker (UUID id)
├── dto/                                # request / response records
│   ├── MapViewConfig.java
│   ├── CreateMarkerRequest.java
│   ├── UpdateMarkerLocationRequest.java
│   └── MarkerResponse.java
└── exception/                          # central error handling
    ├── GlobalExceptionHandler.java     # @RestControllerAdvice
    ├── ApiError.java                   # error payload record
    └── MarkerNotFoundException.java
src/main/resources
├── application.yml                     # dev / staging / prod profiles
└── static                              # ArcGIS front-end (HTML/CSS/JS)
examples/
├── api-examples.sh                     # curl script exercising the API
└── MarkerApiClient.java                # standalone Java client (create a marker)
```

## Configuration Profiles

| Profile      | Database            | Connection pool | Log level |
|--------------|---------------------|-----------------|-----------|
| `dev` (default) | H2 in-memory     | —               | `DEBUG`   |
| `staging`    | PostgreSQL          | 10              | `INFO`    |
| `prod`       | PostgreSQL          | 30              | `WARN`    |

Staging and prod read the datasource from environment variables:
`JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Build & Run

### Prerequisites
- JDK 21
- Maven 3.9+ (or use the bundled `mvnw` if generated)
- Docker (optional, for containerised runs)

### Run locally (dev profile, H2)

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080/> to view the map.

The dev profile enables the H2 console at
<http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:mapdb`, user `sa`,
empty password).

> Spring Security generates a random password on startup (printed in the log).
> The map page, static assets and health endpoint are public, so no login is
> required to view the map.

### Build the jar

```bash
mvn clean package
java -jar target/map-app-0.0.1-SNAPSHOT.jar
```

### Run a specific profile

```bash
java -jar target/map-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=staging
```

### Run the tests

```bash
mvn test
```

## Docker

The multi-stage `Dockerfile` builds with Maven and runs on a slim
`eclipse-temurin:25-jre` image as a non-root user. It defaults to the `prod`
profile.

### Build the image

```bash
docker build -t map-app .
```

### Run the container

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  map-app
```

For a real `prod` run, supply the database connection:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JDBC_URL="jdbc:postgresql://db:5432/mapdb" \
  -e DB_USERNAME="mapapp" \
  -e DB_PASSWORD="secret" \
  map-app
```

## Endpoints

| Method | Path                  | Description                       | Auth |
|--------|-----------------------|-----------------------------------|------|
| GET    | `/`                   | Map viewer page                   | public |
| GET    | `/api/v1/map/config`  | Initial map view configuration    | public |
| GET    | `/api/v1/map/markers` | List all markers                  | public |
| POST   | `/api/v1/map/markers` | Create a marker at a latitude / longitude | public |
| PUT    | `/api/v1/map/markers/{id}/location` | Move a marker to a new location | public |
| DELETE | `/api/v1/map/markers/{id}` | Delete a marker              | public |
| GET    | `/actuator/health`    | Health check                      | public |
| GET    | `/actuator/metrics`   | Metrics                           | authenticated |

### Markers

A marker is a point (`latitude`, `longitude`) with a symbol (`color` as a
`#RRGGBB` hex string, `size`) and an optional `label`. Coordinates are
validated to the `-90..90` / `-180..180` ranges; `color`, `size` and `label`
are optional and fall back to the server defaults (`#E23131`, size `12`).

```bash
# Create a marker (minimal body)
curl -X POST http://localhost:8080/api/v1/map/markers \
  -H "Content-Type: application/json" \
  -d '{"latitude": 51.5072, "longitude": -0.1276}'

# Move an existing marker
curl -X PUT http://localhost:8080/api/v1/map/markers/{id}/location \
  -H "Content-Type: application/json" \
  -d '{"latitude": 52.5200, "longitude": 13.4050}'

# Delete a marker (204 No Content on success, 404 if unknown)
curl -X DELETE http://localhost:8080/api/v1/map/markers/{id}
```

The `examples/api-examples.sh` script exercises every endpoint (including the
`400` and `404` error paths) end-to-end:

```bash
mvn spring-boot:run          # in one terminal
./examples/api-examples.sh   # in another (honours BASE_URL)
```

For a programmatic example, `examples/MarkerApiClient.java` is a standalone,
dependency-free Java client that creates a marker by POSTing to
`/api/v1/map/markers`, then deletes it again via `DELETE
/api/v1/map/markers/{id}`, printing the response at each step. It uses only the
JDK's `java.net.http.HttpClient`, so it runs directly via the Java 21
single-file source launcher — no build step:

```bash
mvn spring-boot:run                       # in one terminal
java examples/MarkerApiClient.java        # in another (defaults to localhost:8080)
java examples/MarkerApiClient.java http://localhost:8080   # optional base URL
```

> Markers persist in the dev H2 in-memory database, so they survive page
> reloads but not an application restart.
