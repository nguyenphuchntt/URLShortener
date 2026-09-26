import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { redirect } from './lib/common.js';

// T3 — Stress: ramp load in steps to find the breaking point.
// Reads warm codes from perf/k6/data/hot.json (cache HIT path).
//
//   k6 run -e BASE_URL=http://localhost:8080 \
//          --summary-export=perf/results/t3.json perf/k6/t3_stress.js
//
// Breaking point = the first step where error rate > 1% OR avg latency jumps
// to ~3× the previous step. Watch terminal output / summary.json per stage.
//
// Override the ramp with -e STAGES='0:0,2m:50,2m:100,2m:400,2m:800'.

const hotCodes = new SharedArray('hot codes', () =>
  JSON.parse(open('./data/hot.json'))
);

// Parse STAGES="0:0,2m:50,2m:100" into k6 stage objects.
function parseStages(spec) {
  if (!spec) {
    return [
      { duration: '2m', target: 50 },
      { duration: '2m', target: 100 },
      { duration: '2m', target: 200 },
      { duration: '2m', target: 400 },
      { duration: '2m', target: 800 },
      { duration: '1m', target: 0 },
    ];
  }
  return spec.split(',').map((pair) => {
    const [duration, target] = pair.split(':');
    return { duration, target: Number(target) };
  });
}

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus', // closed-ish model is fine for breakpoint hunting
      startVUs: 0,
      stages: parseStages(__ENV.STAGES),
      gracefulRampDown: '30s',
    },
  },
  // No thresholds here on purpose: the goal is to observe where it breaks,
  // not to assert a pass/fail line.
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  if (hotCodes.length === 0) {
    throw new Error('perf/k6/data/hot.json is empty — run seed.js first.');
  }
}

export default function () {
  const code = hotCodes[(__VU + __ITER) % hotCodes.length];
  const res = redirect(code);
  check(res, {
    'status 302': (r) => r.status === 302,
    'not rate-limited (429)': (r) => r.status !== 429,
  });
}
