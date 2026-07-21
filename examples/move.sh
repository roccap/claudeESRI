#!/usr/bin/env bash
#
# Stream a few moving symbols to the map-symbols Kafka topic so they animate on
# the map (open http://localhost:8080 while this runs). Each symbol keeps the
# same id across updates, so the map moves the existing symbol in place.
#
# Run it inside the Kafka container provisioned by docker-compose.yml:
#
#   docker cp examples/move.sh kafka:/tmp/move.sh
#   docker exec kafka bash -c "tr -d '\r' < /tmp/move.sh | bash"
#
# Or, if you have the Kafka CLI locally, run it directly:
#
#   BOOTSTRAP=localhost:9092 ./examples/move.sh
#
# Environment overrides:
#   BOOTSTRAP  Kafka bootstrap servers (default localhost:9092)
#   TOPIC      target topic           (default map-symbols)
#   PRODUCER   kafka-console-producer path (default /opt/kafka/bin/kafka-console-producer.sh)
#   STEPS      number of update ticks (default 20)
#   INTERVAL   seconds between ticks  (default 2)

set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-localhost:9092}"
TOPIC="${TOPIC:-map-symbols}"
PRODUCER="${PRODUCER:-/opt/kafka/bin/kafka-console-producer.sh}"
STEPS="${STEPS:-20}"
INTERVAL="${INTERVAL:-2}"

{
  for i in $(seq 0 "$STEPS"); do
    # Three symbols tracing straight paths across London.
    lon1=$(awk -v i="$i" 'BEGIN{printf "%.4f", -0.34 + i*0.026}')   # red circle: west -> east
    lat2=$(awk -v i="$i" 'BEGIN{printf "%.4f", 51.61 - i*0.010}')   # blue square: north -> south
    lon3=$(awk -v i="$i" 'BEGIN{printf "%.4f", -0.24 + i*0.018}')   # green triangle: SW -> NE
    lat3=$(awk -v i="$i" 'BEGIN{printf "%.4f", 51.43 + i*0.007}')
    echo "{\"id\":\"s1\",\"latitude\":51.53,\"longitude\":$lon1,\"shape\":\"circle\",\"color\":\"#E53935\"}"
    echo "{\"id\":\"s2\",\"latitude\":$lat2,\"longitude\":-0.06,\"shape\":\"square\",\"color\":\"#1E88E5\"}"
    echo "{\"id\":\"s3\",\"latitude\":$lat3,\"longitude\":$lon3,\"shape\":\"triangle\",\"color\":\"#43A047\"}"
    sleep "$INTERVAL"
  done
} | "$PRODUCER" --bootstrap-server "$BOOTSTRAP" --topic "$TOPIC"

echo "done streaming symbols"
