// Example client for the Map Viewer Service marker API.
//
// Adds a new marker by POSTing to /api/v1/map/markers, then deletes it again
// with DELETE /api/v1/map/markers/{id}, printing what the service returns at
// each step.
//
// This is a standalone, dependency-free example: it uses only the JDK's
// java.net.http.HttpClient, so it can be run directly with the Java 21
// single-file source launcher — no Maven build required:
//
//     # start the service first (from the project root):
//     ./mvnw spring-boot:run
//
//     # then, in another terminal, run this client:
//     java examples/MarkerApiClient.java
//
//     # optionally override the base URL (e.g. a remote host):
//     java examples/MarkerApiClient.java http://localhost:8080
//
// The marker endpoints are public, so no authentication is required.

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MarkerApiClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String MARKERS_PATH = "/api/v1/map/markers";

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? stripTrailingSlash(args[0]) : DEFAULT_BASE_URL;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String markerId = createMarker(client, baseUrl);
        System.out.println();
        deleteMarker(client, baseUrl, markerId);
    }

    /**
     * Create a marker and return its server-assigned id (parsed from the
     * Location header of the 201 response).
     */
    private static String createMarker(HttpClient client, String baseUrl) throws Exception {
        // latitude/longitude are required; label, color, size and shape are
        // optional and fall back to server defaults (color #E23131, size 12,
        // shape circle) when omitted. shape is one of: circle, square, diamond,
        // triangle, cross, x.
        //
        // JSON is hand-built here to keep the example dependency-free; a real
        // client would use Jackson, Gson, or Spring's RestClient/WebClient.
        String requestBody = """
                {
                  "latitude": 51.5072,
                  "longitude": -0.1276,
                  "label": "London Eye",
                  "color": "#1E88E5",
                  "size": 20,
                  "shape": "diamond"
                }""";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + MARKERS_PATH))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        System.out.println("POST " + request.uri());
        System.out.println("Request body:");
        System.out.println(requestBody);
        System.out.println();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        System.out.println("HTTP " + status);

        String location = response.headers().firstValue("Location").orElse(null);
        if (location != null) {
            System.out.println("Location: " + location);
        }

        System.out.println("Response body:");
        System.out.println(response.body());

        if (status != 201) {
            System.out.println();
            System.out.println("Marker was not created (expected HTTP 201). "
                    + "See the response body above for details.");
            System.exit(1);
        }
        if (location == null) {
            System.out.println();
            System.out.println("Marker created but no Location header was returned; "
                    + "cannot determine its id to delete.");
            System.exit(1);
        }

        System.out.println();
        System.out.println("Marker created successfully.");

        // The Location header is /api/v1/map/markers/{id}.
        return location.substring(location.lastIndexOf('/') + 1);
    }

    /**
     * Delete the given marker, expecting 204 No Content.
     */
    private static void deleteMarker(HttpClient client, String baseUrl, String markerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + MARKERS_PATH + "/" + markerId))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .DELETE()
                .build();

        System.out.println("DELETE " + request.uri());

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        System.out.println("HTTP " + status);
        if (!response.body().isBlank()) {
            System.out.println("Response body:");
            System.out.println(response.body());
        }

        if (status == 204) {
            System.out.println();
            System.out.println("Marker deleted successfully.");
        } else {
            System.out.println();
            System.out.println("Marker was not deleted (expected HTTP 204). "
                    + "See the response above for details.");
            System.exit(1);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
