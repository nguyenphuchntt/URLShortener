#!/usr/bin/env bash
# Runs the existing redirect scenarios, or the opt-in cold-data stress scenario.
#
# Legacy suite:
#   BASE_URL=http://localhost:8080 bash performance_testing/k6/run.sh
#
# T4 only:
#   RUN_T4=true BASE_URL=http://localhost:8080 bash performance_testing/k6/run.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="${RESULTS_DIR:-performance_testing/results}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DATA_DIR="${DATA_DIR:-$SCRIPT_DIR/data/}"

mkdir -p "$RESULTS_DIR" "$DATA_DIR"

if [[ "${RUN_T4:-false}" == "true" ]]; then
  T4_DATA_FILE="${T4_DATA_FILE:-$DATA_DIR/cold.json}"
  if [[ "$T4_DATA_FILE" != /* ]]; then
    T4_DATA_FILE="$PROJECT_ROOT/$T4_DATA_FILE"
  fi
  T4_RESULT="${T4_RESULT:-$RESULTS_DIR/t4-cold-stress.json}"
  if [[ "$T4_RESULT" != /* ]]; then
    T4_RESULT="$PROJECT_ROOT/$T4_RESULT"
  fi
  T4_CODES_PER_VU="${T4_CODES_PER_VU:-5000}"
  T4_STAGES="${T4_STAGES:-2m:25,5m:50,5m:100,5m:200,5m:400,5m:800,5m:1000}"

  if [[ ! -s "$T4_DATA_FILE" ]]; then
    echo "T4_DATA_FILE not found or empty: $T4_DATA_FILE" >&2
    echo "Seed the cold pool first; this test does not seed data." >&2
    exit 1
  fi

  echo "==> T4: redirect-only cold-data capacity test"
  echo "    Using pre-seeded cold pool: $T4_DATA_FILE"
  k6 run -e BASE_URL="$BASE_URL" -e DATA_FILE="$T4_DATA_FILE" \
    -e CODES_PER_VU="$T4_CODES_PER_VU" -e STAGES="$T4_STAGES" \
    --summary-export="$T4_RESULT" "$SCRIPT_DIR/t4_redirect_cold_stress.js"

  echo "==> T4 summary: $T4_RESULT"
  exit 0
fi

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
echo "    T4 is opt-in: RUN_T4=true BASE_URL=$BASE_URL bash $SCRIPT_DIR/run.sh"
