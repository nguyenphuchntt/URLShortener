import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, JSON_HEADERS } from './lib/common.js';

// Seeds test data for the redirect perf scenarios and writes the short codes to
// perf/k6/data/hot.json and perf/k6/data/cold.json.
//
//   k6 run -e BASE_URL=http://localhost:8080 \
//          -e USERNAME=perf -e PASSWORD='Perf@12345' -e EMAIL=perf@example.com \
//          -e HOT=100 -e COLD=10000 perf/k6/seed.js
//
// HOT links get one warm-up redirect each so they sit in the Redis cache (for T1).
// COLD links are created but never redirected, so their first hit is a cache MISS (for T2).
//
// All work happens in setup() and the codes are written from handleSummary(data),
// because that is the only place k6 lets a script write files.

const USERNAME = __ENV.USERNAME || 'perfuser';
const PASSWORD = __ENV.PASSWORD || 'Perf@12345';
const EMAIL = __ENV.EMAIL || 'perfuser@example.com';
const HOT = Number(__ENV.HOT || 100);
const COLD = Number(__ENV.COLD || 10000);
const REDIRECT_PATH = __ENV.REDIRECT_PATH || '/r/';

export const options = {
  scenarios: {
    // Actual load is irrelevant here — setup() does the work, this is a placeholder.
    seed: { executor: 'shared-iterations', vus: 1, iterations: 1, maxDuration: '30m' },
  },
};

// Register (ignore "already exists"), then log in and return the access token.
function authenticate() {
  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ username: USERNAME, email: EMAIL, password: PASSWORD }),
    { headers: JSON_HEADERS }
  );
  if (registerRes.status === 201) {
    const token = registerRes.json('accessToken');
    if (token) return token;
  }
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: JSON_HEADERS }
  );
  check(loginRes, { 'login 200': (r) => r.status === 200 });
  return loginRes.json('accessToken');
}

function createLink(token, label) {
  const res = http.post(
    `${BASE_URL}/api/v1/urls`,
    JSON.stringify({ originUrl: `https://example.com/perf/${label}/${Date.now()}` }),
    { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } }
  );
  if (res.status !== 201) return null;
  return res.json('shortCode');
}

export function setup() {
  const token = authenticate();
  if (!token) {
    throw new Error('Could not authenticate seed user — check credentials / rate limit.');
  }

  const hotCodes = [];
  for (let i = 0; i < HOT; i++) {
    const code = createLink(token, `hot-${i}`);
    if (code) {
      hotCodes.push(code);
      http.get(`${BASE_URL}${REDIRECT_PATH}${code}`, { redirects: 0 }); // warm the cache
    }
  }

  const coldCodes = [];
  for (let i = 0; i < COLD; i++) {
    const code = createLink(token, `cold-${i}`);
    if (code) coldCodes.push(code); // never redirected → stays a cache MISS
  }

  console.log(`Seeded ${hotCodes.length} hot / ${coldCodes.length} cold codes.`);
  return { hotCodes, coldCodes };
}

export default function () {
  // No load work — everything happens in setup().
}

export function handleSummary(data) {
  const { hotCodes, coldCodes } = data.setup_data || {};
  // Defaults to the sibling data/ folder; override with -e DATA_DIR=<existing dir>/.
  const dir = __ENV.DATA_DIR || 'performance_testing/k6/data/';
  return {
    [`${dir}hot.json`]: JSON.stringify(hotCodes || []),
    [`${dir}cold.json`]: JSON.stringify(coldCodes || []),
    stdout: `Seed complete: ${(hotCodes || []).length} hot, ${(coldCodes || []).length} cold → ${dir}\n`,
  };
}
