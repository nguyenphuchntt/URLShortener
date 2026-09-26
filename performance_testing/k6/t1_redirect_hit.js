import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { redirect } from './lib/common.js';

// T1 — Redirect, cache HIT (the hot path).
// Reads warm codes from perf/k6/data/hot.json (produced by seed.js) and hammers them.
//
//   k6 run -e BASE_URL=http://localhost:8080 \
//          --summary-export=perf/results/t1.json perf/k6/t1_redirect_hit.js
//
// Tune load with -e RATE (req/s) and -e DURATION.
//
// ⚠️ EXPECT SPURIOUS 429s UNTIL YOU DISABLE THE RATE LIMITER.
// RateLimitFilter caps every IP at 100 req/min by default and is not configurable
// via properties (see docs/perf_testing.md §3.1). At 20 req/s you burn the budget in
// 5 seconds and then ~2/3 of samples come back 429. Fix one of:
//   (a) raise the limit:  -e RATELIMIT_DEFAULTLIMIT_CAPACITY=1000000 on the backend
//   (b) disable the filter with the "perf" profile (add @Profile("!perf"))
// Until then http_req_failed will be dominated by 429 and the numbers are meaningless.
// A 429 counts as a check failure here, which is what trips the threshold below.

const RATE = Number(__ENV.RATE || 1000);
const DURATION = __ENV.DURATION || '5m';

// SharedArray loads the file once and shares it across all VUs (memory-efficient).
const hotCodes = new SharedArray('hot codes', () =>
  JSON.parse(open('./data/hot.json'))
);

export const options = {
  scenarios: {
    redirect_hit: {
      executor: 'constant-arrival-rate', // open model: overload shows up as a latency/error spike
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Number(__ENV.PRE_VUS || 100),
      maxVUs: Number(__ENV.MAX_VUS || 500),
    },
  },
  // Force avg/p90/p99 into --summary-export (otherwise only med/p90/p95 land there).
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.001'],
    http_req_duration: ['avg<10', 'p(90)<20', 'p(99)<40'],
  },
};

export function setup() {
  if (hotCodes.length === 0) {
    throw new Error('perf/k6/data/hot.json is empty — run seed.js first.');
  }
}

export default function () {
  // Spread across all hot codes so no single Redis key is unfairly hot.
  const code = hotCodes[(__VU + __ITER) % hotCodes.length];
  const res = redirect(code);
  check(res, {
    'status 302': (r) => r.status === 302,
    'not rate-limited (429)': (r) => r.status !== 429,
  });
}
