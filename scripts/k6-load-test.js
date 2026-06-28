import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, {
    'health status 200': (r) => r.status === 200,
    'health UP': (r) => r.json('status') === 'UP',
  });

  const unidades = http.get(`${BASE_URL}/unidades`);
  check(unidades, {
    'unidades status 200': (r) => r.status === 200,
  });

  const produtos = http.get(`${BASE_URL}/produtos?unidadeId=1`);
  check(produtos, {
    'produtos status 200': (r) => r.status === 200,
  });

  sleep(0.2);
}
