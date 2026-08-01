import http from 'k6/http';
import { check, sleep } from 'k6';

const vus = parseInt(__ENV.VUS) || 200;

export const options = {
  stages: [
    { duration: '30s', target: vus },
    { duration: '1m', target: vus },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<1000'],
  },
};

const BASE_URL = 'http://localhost:8080/api/v1/tickets';

export default function () {
  const res = http.get(`${BASE_URL}?page=0&size=20`);
  check(res, { 'status was 200': (r) => r.status == 200 });
  sleep(1);
}