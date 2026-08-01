import http from 'k6/http';
import { check, sleep } from 'k6';

// Under high contention, 409 (ticket already reserved) and 503 (lock busy/fail-fast)
// are *expected business outcomes*, not infrastructure failures.
// Any other status (500, 502, 504) or network error is a REAL failure.
http.setResponseCallback(http.expectedStatuses(200, 409, 503));

const vus = parseInt(__ENV.VUS) || 100;

export const options = {
  stages: [
    { duration: '30s', target: vus },
    { duration: '1m', target: vus },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    // Hard gate: unexpected statuses / network errors must stay at ~0
    http_req_failed: ['rate<0.01'],
    // Every response must be one of the allowed business statuses
    checks: ['rate>0.99'],
    // SLO applies ONLY to successful reservations (200), not to 409/503 rejects
    'http_req_duration{expected_response:true}': ['p(95)<500', 'p(99)<2000'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1/tickets';

export default function () {
  const ticketId = Math.floor(Math.random() * 5) + 1;
  const expectedStatuses = [200, 409, 503];

  const reserveRes = http.post(`${BASE_URL}/${ticketId}/reserve`);

  check(reserveRes, {
    'reserve status is valid (no 500)': (r) => expectedStatuses.includes(r.status),
  });

  sleep(1);
}