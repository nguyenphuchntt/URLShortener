#!/usr/bin/env bash
# Runs the redirect perf scenarios in order and drops summaries into performance_testing/results/.
#
# Usage:
#   BASE_URL=http://localhost:8080 bash performance_testing/k6/run.sh
#
# Prereqs (see docs/perf_testing.md):
#   - rate limit disabled (RATELIMIT_ENABLED=false in .env, then docker compose up)
#   - k6 running on a DIFFERENT machine than the backend
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="${RESULTS_DIR:-performance_testing/results}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="${DATA_DIR:-$SCRIPT_DIR/data/}"

mkdir -p "$RESULTS_DIR" "$DATA_DIR"

echo "==> Seeding data (hot/cold links)"
k6 run -e BASE_URL="$BASE_URL" -e DATA_DIR="$DATA_DIR" \
  -e HOT="${HOT:-100}" -e COLD="${COLD:-10000}" \
  "$SCRIPT_DIR/seed.js"

echo "==> T1: redirect cache HIT"
k6 run -e BASE_URL="$BASE_URL" -e RATE="${T1_RATE:-1000}" -e DURATION="${T1_DURATION:-5m}" \
  --summary-export="$RESULTS_DIR/t1.json" "$SCRIPT_DIR/t1_redirect_hit.js"

echo "==> T2: redirect cache MISS"
k6 run -e BASE_URL="$BASE_URL" -e VUS="${T2_VUS:-20}" -e ITERATIONS="${T2_ITERATIONS:-2000}" \
  --summary-export="$RESULTS_DIR/t2.json" "$SCRIPT_DIR/t2_redirect_miss.js"

echo "==> T3: stress (ramp to breaking point)"
k6 run -e BASE_URL="$BASE_URL" \
  --summary-export="$RESULTS_DIR/t3.json" "$SCRIPT_DIR/t3_stress.js"

echo "==> Done. Summaries in $RESULTS_DIR/"
echo "    Read avg/p90/p99 from each JSON: metrics.http_req_duration.values"
