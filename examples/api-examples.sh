#!/usr/bin/env bash
#
# Exercises the Map Viewer Service REST API end-to-end with curl.
#
# Usage:
#   ./examples/api-examples.sh                 # against http://localhost:8080
#   BASE_URL=http://host:port ./examples/api-examples.sh
#
# The API is under /api/v1/map and is permitAll + CSRF-exempt, so no auth
# or CSRF token is needed. Start the app first:  mvn spring-boot:run
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="${BASE_URL}/api/v1/map"

# Pretty-print JSON if jq is available, otherwise pass through untouched.
pp() { if command -v jq >/dev/null 2>&1; then jq .; else cat; fi; }

hr() { printf '\n=== %s ===\n' "$1"; }

# ---------------------------------------------------------------------------
hr "GET initial map view config"
curl -s "${API}/config" | pp

# ---------------------------------------------------------------------------
hr "GET all markers (before)"
curl -s "${API}/markers" | pp

# ---------------------------------------------------------------------------
hr "POST create a marker (London) — minimal body (lat/long only)"
CREATED=$(curl -s -X POST "${API}/markers" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 51.5072, "longitude": -0.1276}')
echo "${CREATED}" | pp

# Extract the generated UUID (works with or without jq).
if command -v jq >/dev/null 2>&1; then
  ID=$(echo "${CREATED}" | jq -r '.id')
else
  ID=$(echo "${CREATED}" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
fi
echo "created id: ${ID}"

# ---------------------------------------------------------------------------
hr "POST create a marker — full body (label, color, size)"
curl -s -X POST "${API}/markers" \
  -H "Content-Type: application/json" \
  -d '{
        "latitude": 48.8566,
        "longitude": 2.3522,
        "label": "Paris",
        "color": "#2E7D32",
        "size": 18
      }' | pp

# ---------------------------------------------------------------------------
hr "PUT move the first marker to a new location (London -> Berlin)"
curl -s -X PUT "${API}/markers/${ID}/location" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 52.5200, "longitude": 13.4050}' | pp

# ---------------------------------------------------------------------------
hr "GET all markers (after)"
curl -s "${API}/markers" | pp

# ---------------------------------------------------------------------------
hr "POST validation error -> 400 (latitude out of range)"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X POST "${API}/markers" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 999, "longitude": -0.1276}'
curl -s -X POST "${API}/markers" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 999, "longitude": -0.1276}' | pp

# ---------------------------------------------------------------------------
hr "PUT move a non-existent marker -> 404"
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X PUT \
  "${API}/markers/00000000-0000-0000-0000-000000000000/location" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 10, "longitude": 10}'
curl -s -X PUT \
  "${API}/markers/00000000-0000-0000-0000-000000000000/location" \
  -H "Content-Type: application/json" \
  -d '{"latitude": 10, "longitude": 10}' | pp

hr "done"
