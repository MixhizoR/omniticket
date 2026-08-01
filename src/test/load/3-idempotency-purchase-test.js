import http from 'k6/http';
import { check, sleep } from 'k6';

// reserve/purchase: 409 (ticket already reserved/sold) and 503 (lock busy)
// are expected business outcomes. 500/502/504/network errors are REAL failures.
http.setResponseCallback(http.expectedStatuses(200, 409, 503));

const vus = parseInt(__ENV.VUS) || 50;

export const options = {
  stages: [
    { duration: '30s', target: vus },
    { duration: '1m', target: vus },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
    http_req_duration: ['p(99)<2000'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1/tickets';

export default function () {
  const ticketId = Math.floor(Math.random() * 5) + 1;
  const idempotencyKey = `k6-key-${__VU}-${__ITER}`;
  const expectedStatuses = [200, 409, 503];

  const reserveRes = http.post(`${BASE_URL}/${ticketId}/reserve`);
  check(reserveRes, { 'reserve valid': (r) => expectedStatuses.includes(r.status) });

  if (reserveRes.status === 200) {
    // 1. Satın alma denemesi
    const purchaseRes = http.post(`${BASE_URL}/${ticketId}/purchase`, null, {
      headers: { 'Idempotency-Key': idempotencyKey },
    });
    check(purchaseRes, { 'purchase valid': (r) => expectedStatuses.includes(r.status) });

    // 2. IDEMPOTENCY TESTİ: Aynı key ile tekrar iste
    if (purchaseRes.status === 200) {
      const duplicateRes = http.post(`${BASE_URL}/${ticketId}/purchase`, null, {
        headers: { 'Idempotency-Key': idempotencyKey },
      });

      check(duplicateRes, {
        'idempotent returns 200': (r) => r.status === 200,
        'same ticket id returned': (r) => r.json('id') === ticketId,
      });
    }
  }
  sleep(1);
}