#!/usr/bin/env bash
# Seed T4 cold links directly into PostgreSQL and export their codes for k6.
# The dedicated owner is registered through the API only when it does not exist.
set -euo pipefail

COUNT="${T4_COLD:?T4_COLD must be set}"
OUTPUT="${T4_DATA_FILE:?T4_DATA_FILE must be set}"
BATCH_SIZE="${T4_DB_BATCH_SIZE:-10000}"
DB_SERVICE="${T4_DB_SERVICE:-db}"
OWNER_USERNAME="${T4_OWNER_USERNAME:-perfuser}"
OWNER_EMAIL="${T4_OWNER_EMAIL:-${OWNER_USERNAME}@example.com}"
OWNER_PASSWORD="${T4_OWNER_PASSWORD:-Perf@12345}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
NAMESPACE="${T4_NAMESPACE:-t4$(date +%s | tail -c 7)$(printf '%06x' "$RANDOM")}"
if [[ -z "${T4_NAMESPACE:-}" ]]; then
  NAMESPACE="$(printf '%s' "$NAMESPACE" | cut -c1-8)"
fi

[[ "$COUNT" =~ ^[1-9][0-9]*$ ]] || { echo "T4_COLD must be a positive integer" >&2; exit 2; }
[[ "$BATCH_SIZE" =~ ^[1-9][0-9]*$ ]] || { echo "T4_DB_BATCH_SIZE must be a positive integer" >&2; exit 2; }
[[ "$NAMESPACE" =~ ^[a-zA-Z0-9]{1,8}$ ]] || { echo "T4_NAMESPACE must be 1-8 alphanumeric characters" >&2; exit 2; }
mkdir -p "$(dirname "$OUTPUT")"
tmp_output="${OUTPUT}.tmp.$$"
trap 'rm -f "$tmp_output" "$tmp_output.codes"' EXIT

DB_USER="${POSTGRES_USER:-$(docker compose exec -T "$DB_SERVICE" printenv POSTGRES_USER | tr -d '\r')}"
DB_NAME="${POSTGRES_DB:-$(docker compose exec -T "$DB_SERVICE" printenv POSTGRES_DB | tr -d '\r')}"
DB_USER="${DB_USER:-urlshortener}"
DB_NAME="${DB_NAME:-urlshortener}"
psql() { docker compose exec -T "$DB_SERVICE" psql -X -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" "$@"; }

# Create the dedicated account through the public API only if it is absent.
owner_count="$(psql -Atc "SELECT count(*) FROM users WHERE username = '$(printf '%s' "$OWNER_USERNAME" | sed "s/'/''/g")';")"
if [[ "$owner_count" == "0" ]]; then
  echo "==> Registering dedicated T4 owner $OWNER_USERNAME via $BASE_URL"
  curl --fail-with-body --silent --show-error -X POST "$BASE_URL/api/v1/auth/register" \
    -H 'Content-Type: application/json' \
    --data "$(printf '{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}' \
      "${OWNER_USERNAME//\/\\}" "${OWNER_EMAIL//\/\\}" "${OWNER_PASSWORD//\/\\}")" >/dev/null
fi

owner_id="$(psql -Atc "SELECT id FROM users WHERE username = '$(printf '%s' "$OWNER_USERNAME" | sed "s/'/''/g")';")"
[[ "$owner_id" =~ ^[0-9]+$ ]] || { echo "Could not resolve T4 owner $OWNER_USERNAME" >&2; exit 1; }

existing="$(psql -Atc "SELECT count(*) FROM short_urls WHERE short_code LIKE '${NAMESPACE}%';")"
[[ "$existing" == "0" ]] || { echo "Namespace $NAMESPACE already exists; choose T4_NAMESPACE" >&2; exit 1; }

echo "==> Direct seeding $COUNT cold links (owner id $owner_id, namespace $NAMESPACE)"
for ((offset=0; offset<COUNT; offset+=BATCH_SIZE)); do
  limit=$((COUNT - offset)); (( limit > BATCH_SIZE )) && limit=$BATCH_SIZE
  psql -v namespace="$NAMESPACE" -v owner_id="$owner_id" -v offset="$offset" -v limit="$limit" <<'SQL'
INSERT INTO short_urls (short_code, origin_url, owner_id, status, expires_at, created_at, updated_at)
SELECT :'namespace' || lpad(to_hex(g), 8, '0'),
       'https://example.com/perf/t4/' || :'namespace' || '/' || g,
       :'owner_id'::bigint, 'ACTIVE'::short_url_status, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM generate_series(:'offset'::bigint, (:'offset'::bigint + :'limit'::bigint - 1)) AS g;
SQL
  printf '\r[seed] %d/%d' "$((offset + limit))" "$COUNT" >&2
done
printf '\n' >&2

# Export in deterministic insertion/index order without materializing the JSON in bash.
psql -Atc "SELECT short_code FROM short_urls WHERE short_code LIKE '${NAMESPACE}%' ORDER BY short_code;" >"$tmp_output.codes"
python3 - "$tmp_output.codes" "$tmp_output" "$COUNT" <<'PY'
import json, os, sys
source, target, expected = sys.argv[1], sys.argv[2], int(sys.argv[3])
count = 0
with open(target, 'w', encoding='utf-8') as out:
    out.write('[')
    first = True
    with open(source, encoding='utf-8') as codes:
        for line in codes:
            code = line.strip()
            if not code:
                continue
            if not first:
                out.write(',')
            out.write(json.dumps(code, ensure_ascii=False))
            first = False
            count += 1
    out.write(']\n')
if count != expected:
    os.unlink(target)
    raise SystemExit(f'exported {count} codes, expected {expected}')
PY
mv -f "$tmp_output" "$OUTPUT"
echo "==> Wrote $COUNT cold codes to $OUTPUT"
