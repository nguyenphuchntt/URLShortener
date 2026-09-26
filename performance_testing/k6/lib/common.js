import http from 'k6/http';

// Shared config + helpers for all redirect perf scripts.
// Everything is driven by env vars so the same scripts run against local / staging.

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Path prefix must match `short-code.redirect-path` in application.properties (default "/r/").
export const REDIRECT_PATH = __ENV.REDIRECT_PATH || '/r/';

export const JSON_HEADERS = { 'Content-Type': 'application/json' };

// The redirect endpoint answers 302, which k6 treats as a non-2xx (i.e. "failed") by default.
// Declaring 301/302 as expected flips http_req_failed into a meaningful signal: it now reports
// only real problems (5xx, connection errors, and — importantly — 429s), not every success.
http.setResponseCallback(http.expectedStatuses(301, 302));

// GET /r/{code} with redirect following DISABLED.
// redirects:0 is mandatory — otherwise k6 follows the 302 out to the real Internet
// and every sample measures the upstream site instead of this backend.
export function redirect(code) {
  return http.get(`${BASE_URL}${REDIRECT_PATH}${code}`, {
    redirects: 0,
    tags: { name: 'redirect' },
  });
}


// Pick a pseudo-random element; __VU/__ITER keep VUs from hammering the same key.
export function pick(list) {
  return list[(__VU + __ITER) % list.length];
}
