import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';
import { redirect } from './lib/common.js';

// T4 — redirect-only cold-data capacity test.
// The database is seeded before this script starts. This script never seeds data and never
// collects host/database/Redis metrics; k6 reports throughput, p99, status failures, and VU load.
//
// k6 run -e BASE_URL=http://localhost:8080 \
//   -e DATA_FILE=./data/cold.json \
//   -e CODES_PER_VU=5000 \
//   -e STAGES='2m:25,5m:50,5m:100,5m:200,5m:400,5m:800,5m:1000' \
//   --summary-export=../../performance_testing/results/t4-cold-stress.json \
//   performance_testing/k6/t4_redirect_cold_stress.js

const DATA_FILE = __ENV.DATA_FILE || './data/cold.json';
const CODES_PER_VU = Number(__ENV.CODES_PER_VU || 5000);
const WARMUP_INDEX = 0;

function parseStages(spec) {
  const value = spec || '2m:25,5m:50,5m:100,5m:200,5m:400,5m:800,5m:1000';
  const parsed = value.split(',').map((pair, index) => {
    const parts = pair.split(':');
    const duration = (parts[0] || '').trim();
    const target = Number((parts[1] || '').trim());
    if (parts.length !== 2 || !/^\d+(ms|s|m|h)$/.test(duration)
        || !Number.isInteger(target) || target <= 0) {
      throw new Error(`Invalid STAGES item at index ${index}: ${pair}; use duration:positiveVUs`);
    }
    return { duration, target };
  });
  if (parsed.length < 2) throw new Error('STAGES must include warm-up and at least one measurement stage');
  return parsed;
}

function durationSeconds(duration) {
  const match = duration.match(/^(\d+)(ms|s|m|h)$/);
  const amount = Number(match[1]);
  if (match[2] === 'ms') return amount / 1000;
  if (match[2] === 's') return amount;
  if (match[2] === 'm') return amount * 60;
  return amount * 3600;
}

const stages = parseStages(__ENV.STAGES);
const coldCodes = new SharedArray('cold codes', () => JSON.parse(open(DATA_FILE)));

let reservedOffset = 0;
const reservations = stages.map((stage, index) => {
  const reserved = stage.target * CODES_PER_VU;
  const result = { ...stage, index, offset: reservedOffset, reserved };
  reservedOffset += reserved;
  return result;
});

function startTimeSeconds(index) {
  return reservations.slice(0, index)
    .reduce((total, stage) => total + durationSeconds(stage.duration), 0);
}

const scenarios = {};
for (const stage of reservations) {
  scenarios[`stage_${stage.index}`] = {
    executor: 'constant-vus',
    vus: stage.target,
    duration: stage.duration,
    startTime: `${startTimeSeconds(stage.index)}s`,
    exec: `runStage${stage.index}`,
    gracefulStop: '0s',
  };
}

export const options = {
  scenarios,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

const coldDuration = new Trend('redirect_cold_duration', true);
const successful302 = new Counter('redirect_cold_302');
const rateLimited429 = new Counter('redirect_cold_429');
const other4xx = new Counter('redirect_cold_4xx');
const server5xx = new Counter('redirect_cold_5xx');
const networkErrors = new Counter('redirect_cold_network_errors');
const successRate = new Rate('redirect_cold_success_rate');

export function setup() {
  if (!Number.isInteger(CODES_PER_VU) || CODES_PER_VU <= 0) {
    throw new Error(`CODES_PER_VU must be a positive integer, got ${CODES_PER_VU}`);
  }
  if (coldCodes.length === 0) throw new Error(`${DATA_FILE} is empty`);
  if (reservedOffset > coldCodes.length) {
    throw new Error(`Need ${reservedOffset} cold codes, but ${DATA_FILE} contains only ${coldCodes.length}`);
  }
  return { reservations, requiredCodes: reservedOffset, coldPoolSize: coldCodes.length };
}

function runStage(stageIndex) {
  const stage = reservations[stageIndex];
  const codeIndex = stage.offset + (__VU - 1) * CODES_PER_VU + __ITER;
  const code = coldCodes[codeIndex];
  const tags = {
    name: 'redirect_cold',
    stage: `stage_${stageIndex}`,
    phase: stageIndex === WARMUP_INDEX ? 'warmup' : 'measurement',
  };
  if (!code) {
    networkErrors.add(1, tags);
    successRate.add(0, tags);
    return;
  }

  const response = redirect(code, tags);
  coldDuration.add(response.timings.duration, tags);
  if (response.status === 302) {
    successful302.add(1, tags);
    successRate.add(1, tags);
  } else {
    successRate.add(0, tags);
    if (response.status === 0) networkErrors.add(1, tags);
    else if (response.status === 429) rateLimited429.add(1, tags);
    else if (response.status >= 400 && response.status < 500) other4xx.add(1, tags);
    else if (response.status >= 500) server5xx.add(1, tags);
    else other4xx.add(1, tags);
  }
  check(response, { 'cold redirect status 302': (res) => res.status === 302 }, tags);
}

for (let i = 0; i < reservations.length; i++) {
  if (i >= 8) throw new Error('STAGES supports at most 8 stages');
}
export function runStage0() { runStage(0); }
export function runStage1() { runStage(1); }
export function runStage2() { runStage(2); }
export function runStage3() { runStage(3); }
export function runStage4() { runStage(4); }
export function runStage5() { runStage(5); }
export function runStage6() { runStage(6); }
export function runStage7() { runStage(7); }

export function handleSummary(data) {
  const setupData = data.setup_data || {};
  const lines = [
    'T4 redirect-only cold-data capacity test',
    `Required cold codes: ${setupData.requiredCodes || 0}`,
    `Available cold codes: ${setupData.coldPoolSize || 0}`,
    'Stage 0 is warm-up; capacity uses stages 1+.',
    'For each measurement stage: throughput = stage http_reqs / stage duration; p99 = redirect_cold_duration{stage=stage_N} p(99).',
    'Capacity is the highest stage meeting the selected p99 and error-rate SLO while 302 throughput still scales acceptably.',
  ];
  return { stdout: `${lines.join('\n')}\n` };
}
