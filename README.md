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
- Spring Security with sensible public defaults (map page + health endpoint).
- Spring Boot Actuator for health / metrics.
- Spring Retry + AOP enabled for resilient service calls.
- Profile-based configuration for dev, staging and prod.

## Project Layout

```
src/main/java/com/appmcore/mapapp
├── MapAppApplication.java        # entry point (@EnableRetry)
├── config/SecurityConfig.java    # web security rules
├── controller/MapController.java # /api/v1/map/config
└── dto/MapViewConfig.java        # initial view record
src/main/resources
├── application.yml               # dev / staging / prod profiles
└── static                        # ArcGIS front-end (HTML/CSS/JS)
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
| GET    | `/actuator/health`    | Health check                      | public |
| GET    | `/actuator/metrics`   | Metrics                           | authenticated |
