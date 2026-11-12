#!/usr/bin/env bash
# Read-only metrics sampler for redirect cold-data stress runs.
# Samples Docker CPU/RAM for app, PostgreSQL, and Redis, plus Redis/stream and PostgreSQL activity.
# Run in the background before starting k6; stop with `kill <pid>`.
#
#   nohup bash performance_testing/scripts/collect_metrics.sh \
#     --interval 5 \
#     --csv performance_testing/results/t4-infra.csv \
#     > performance_testing/results/t4-monitor.log 2>&1 &
#   echo $! > performance_testing/results/t4-monitor.pid
#
set -euo pipefail

INTERVAL=5
APP_CONTAINER="urlshortener-app"
DB_CONTAINER="urlshortener-db"
REDIS_CONTAINER="urlshortener-redis"
STREAM_KEY="urlshortener:stream:click_events"
STREAM_GROUP="click-analytics"
CSV_OUT="performance_testing/results/t4-infra.csv"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --interval) INTERVAL="$2"; shift 2 ;;
    --app-container) APP_CONTAINER="$2"; shift 2 ;;
    --db-container) DB_CONTAINER="$2"; shift 2 ;;
    --redis-container) REDIS_CONTAINER="$2"; shift 2 ;;
    --stream-key) STREAM_KEY="$2"; shift 2 ;;
    --stream-group) STREAM_GROUP="$2"; shift 2 ;;
    --csv) CSV_OUT="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,14p' "$0"
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

if ! [[ "$INTERVAL" =~ ^[1-9][0-9]*$ ]]; then
  echo "--interval must be a positive integer number of seconds" >&2
  exit 2
fi

mkdir -p "$(dirname "$CSV_OUT")"
printf '%s\n' \
  'timestamp,app_cpu_pct,app_mem_used,app_mem_pct,db_cpu_pct,db_mem_used,db_mem_pct,redis_cpu_pct,redis_mem_used,redis_mem_pct,pg_active,pg_waiting,pg_locks,redis_used_memory_bytes,redis_clients,redis_blocked_clients,redis_ops_per_sec,stream_length,stream_pending' \
  > "$CSV_OUT"

echo "[monitor] sampling app/db/redis every ${INTERVAL}s → $CSV_OUT"

# Convert one Docker stats row to CPU%, memory-used and memory-percent fields.
docker_metrics() {
  local container="$1"
  docker stats --no-stream --format '{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}}' "$container" 2>/dev/null |
    awk -F',' '
      function mib(value, number, unit) {
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        if (match(value, /^[0-9.]+/)) number=substr(value, RSTART, RLENGTH)+0
        unit=substr(value, RLENGTH+1)
        if (unit ~ /^GiB/) return number*1024
        if (unit ~ /^KiB/) return number/1024
        if (unit ~ /^B/) return number/1048576
        return number
      }
      {
        split($2, memory, " / ")
        gsub(/%/, "", $1); gsub(/%/, "", $3)
        printf "%.2f,%.2f,%.2f", $1+0, mib(memory[1]), $3+0
      }'
}

# Redis is queried in its own container. Requirepass may be configured; honor REDIS_PASSWORD
# when provided by the caller without printing it or writing it to the CSV.
redis_cli() {
  if [[ -n "${REDIS_PASSWORD:-}" ]]; then
    docker exec "$REDIS_CONTAINER" redis-cli -a "$REDIS_PASSWORD" --no-auth-warning "$@" 2>/dev/null
  else
    docker exec "$REDIS_CONTAINER" redis-cli "$@" 2>/dev/null
  fi
}

previous_ops=""
while true; do
  timestamp="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  app_metrics="$(docker_metrics "$APP_CONTAINER" || echo ',,')"
  db_metrics="$(docker_metrics "$DB_CONTAINER" || echo ',,')"
  redis_metrics="$(docker_metrics "$REDIS_CONTAINER" || echo ',,')"

  pg_stats="$(docker exec "$DB_CONTAINER" sh -c \
    'psql -X -qAt -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FILTER (WHERE state = '\''active'\''), count(*) FILTER (WHERE wait_event IS NOT NULL), (SELECT count(*) FROM pg_locks WHERE NOT granted) FROM pg_stat_activity WHERE datname=current_database();"' \
    2>/dev/null || true)"
  IFS='|' read -r pg_active pg_waiting pg_locks <<< "$pg_stats"

  redis_memory="$(redis_cli INFO memory | awk -F: '/^used_memory:/{gsub(/\r/,"",$2); print $2}' || true)"
  redis_clients="$(redis_cli INFO clients | awk -F: '/^connected_clients:/{gsub(/\r/,"",$2); print $2}' || true)"
  redis_blocked="$(redis_cli INFO clients | awk -F: '/^blocked_clients:/{gsub(/\r/,"",$2); print $2}' || true)"
  redis_ops="$(redis_cli INFO stats | awk -F: '/^total_commands_processed:/{gsub(/\r/,"",$2); print $2}' || true)"
  stream_length="$(redis_cli XLEN "$STREAM_KEY" | tr -d '\r' || true)"
  stream_pending="$(redis_cli XPENDING "$STREAM_KEY" "$STREAM_GROUP" | awk 'NR==1{print $1}' | tr -d '\r' || true)"

  if [[ "$redis_ops" =~ ^[0-9]+$ && "$previous_ops" =~ ^[0-9]+$ ]]; then
    redis_ops_per_sec=$(( (redis_ops - previous_ops) / INTERVAL ))
  else
    redis_ops_per_sec=""
  fi
  previous_ops="$redis_ops"

  printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
    "$timestamp" "$app_metrics" "$db_metrics" "$redis_metrics" \
    "${pg_active:-}" "${pg_waiting:-}" "${pg_locks:-}" \
    "${redis_memory:-}" "${redis_clients:-}" "${redis_blocked:-}" \
    "$redis_ops_per_sec" "${stream_length:-}" "${stream_pending:-}" >> "$CSV_OUT"

  echo "[monitor] $timestamp app=[$app_metrics] db=[$db_metrics] redis=[$redis_metrics] pg_active=${pg_active:-?} pg_waiting=${pg_waiting:-?} pending=${stream_pending:-?}"
  sleep "$INTERVAL"
done
