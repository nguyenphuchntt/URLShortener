import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { redirect } from './lib/common.js';

// T2 — Redirect, cache MISS (full cost: rate limit + Bloom + DB + cache write + XADD).
// Reads cold codes from perf/k6/data/cold.json (produced by seed.js).
//
// IMPORTANT: a code only misses the cache on its FIRST hit. Once hit, it is cached for
// up to 1h, so the same code served twice is no longer a MISS. Two ways to keep it honest:
//
//   (a) Per-iteration uniqueness — each VU iteration takes a distinct code, so a run of N
//       iterations consumes N cold codes. Run with `iterations <= coldCodes.length`.
//   (b) FLUSHDB between runs — `redis-cli -a "$REDIS_PASSWORD" FLUSHDB`.
//
// This script uses (a): it advances a per-VU cursor so no code is redirected twice.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e VUS=50 -e ITERATIONS=10000 \
//          --summary-export=perf/results/t2.json perf/k6/t2_redirect_miss.js

const VUS = Number(__ENV.VUS || 20);
const ITERATIONS = Number(__ENV.ITERATIONS || 2000);

const coldCodes = new SharedArray('cold codes', () =>
  JSON.parse(open('./data/cold.json'))
);

export const options = {
  scenarios: {
    redirect_miss: {
      executor: 'shared-iterations', // each iteration must use a fresh code → closed model is correct here
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || '30m',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.001'],
    http_req_duration: ['avg<30', 'p(90)<60', 'p(99)<120'],
  },
};

// Give each VU its own disjoint slice of the code pool (start index from __VU).
let cursor = 0;

export function setup() {
  if (coldCodes.length === 0) {
    throw new Error('perf/k6/data/cold.json is empty — run seed.js first.');
  }
  if (ITERATIONS > coldCodes.length) {
    throw new Error(
      `ITERATIONS (${ITERATIONS}) > cold codes (${coldCodes.length}); ` +
      `codes would be reused and stop being cache misses. Seed more with -e COLD=N.`
    );
  }
}

export default function () {
  // Interleave VUs so they don't collide: vu 1 takes 1, VUS+1, 2*VUS+1, ...
  const code = coldCodes[(__VU - 1) + __ITER * VUS];
  if (!code) return;

  const res = redirect(code);
  check(res, {
    'status 302': (r) => r.status === 302,
    'not rate-limited (429)': (r) => r.status !== 429,
  });
}
