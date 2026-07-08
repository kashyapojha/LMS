/**
 * Xebia LMS — Load Test Script (k6)
 * ----------------------------------
 * Simulates concurrent users hitting the real, existing API endpoints:
 *   POST /api/auth/login
 *   GET  /api/categories
 *   GET  /api/categories/{id}/courses
 *   GET  /api/courses/{id}
 *
 * HOW TO RUN
 *   1. Install k6:        https://k6.io/docs/get-started/installation/
 *   2. Set your target:   export BASE_URL="http://<your-ec2-ip>"
 *   3. Run a quick smoke test first (10 users, 30s) to confirm it all works:
 *        k6 run --vus 10 --duration 30s loadtest.js
 *   4. Then run the full 3,000-user ramp test:
 *        k6 run loadtest.js
 *
 * IMPORTANT — run this FROM A DIFFERENT MACHINE than your app server.
 * If you run k6 on the same EC2 instance you're testing, the load generator
 * itself competes for the same CPU/RAM you're trying to measure, and results
 * will be meaningless. Run it from your laptop, a separate small EC2 instance,
 * or a CI runner instead.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ---------- Config ----------
const BASE_URL = 'http://3.108.19.171';

// Only 2 accounts exist in the current backend (AuthController is hardcoded).
// We rotate between them so the login endpoint still gets hit under load,
// but this does NOT simulate 3,000 distinct authenticating users.
const ACCOUNTS = [
  { email: 'admin@xebia.com', password: 'admin123' },
  { email: 'instructor@xebia.com', password: 'instructor123' },
];

// Custom metrics so the summary clearly shows what broke, if anything
const loginFailRate = new Rate('login_failures');
const apiFailRate = new Rate('api_failures');
const loginDuration = new Trend('login_duration', true);
const browseDuration = new Trend('browse_duration', true);

// ---------- Ramp profile: 0 -> 3000 virtual users ----------
// Ramps gradually rather than slamming 3000 users on instantly, so you can see
// the point where things start degrading rather than just "pass" or "crash".
export const options = {
  scenarios: {
    ramping_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 500 },   // warm up
        { duration: '3m', target: 1500 },  // ramp to half target
        { duration: '3m', target: 3000 },  // ramp to full 3,000
        { duration: '5m', target: 3000 },  // hold at 3,000 — this is the real test
        { duration: '2m', target: 0 },     // ramp down
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // Test fails these thresholds if the app can't actually handle the load —
    // treat these as your pass/fail bar, not just a nice-to-have.
    http_req_duration: ['p(95)<2000'],      // 95% of requests under 2s
    http_req_failed: ['rate<0.05'],         // less than 5% hard failures
    login_failures: ['rate<0.05'],
    api_failures: ['rate<0.05'],
  },
};

export default function () {
  const account = ACCOUNTS[Math.floor(Math.random() * ACCOUNTS.length)];

  // ---- Step 1: Login ----
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: account.email, password: account.password }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  );
  loginDuration.add(loginRes.timings.duration);
  const loginOk = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
  });
  loginFailRate.add(!loginOk);

  sleep(randomThinkTime(1, 3)); // simulate a human pausing after login

  // ---- Step 2: Browse categories ----
  const categoriesRes = http.get(`${BASE_URL}/api/categories`, { tags: { name: 'categories' } });
  browseDuration.add(categoriesRes.timings.duration);
  const categoriesOk = check(categoriesRes, {
    'categories status is 200': (r) => r.status === 200,
  });
  apiFailRate.add(!categoriesOk);

  sleep(randomThinkTime(1, 2));

  // ---- Step 3: Open a category's courses (only if we got real category data back) ----
  let categoryId = null;
  try {
    const body = JSON.parse(categoriesRes.body);
    const list = body?.data || body; // handles either {data:[...]} or [...] shapes
    if (Array.isArray(list) && list.length > 0) {
      categoryId = list[Math.floor(Math.random() * list.length)].id;
    }
  } catch (e) {
    // response wasn't JSON / didn't match expected shape — treat as a failed step
    apiFailRate.add(true);
  }

  if (categoryId) {
    const coursesRes = http.get(`${BASE_URL}/api/categories/${categoryId}/courses`, {
      tags: { name: 'category_courses' },
    });
    const coursesOk = check(coursesRes, {
      'category courses status is 200': (r) => r.status === 200,
    });
    apiFailRate.add(!coursesOk);

    sleep(randomThinkTime(1, 2));

    // ---- Step 4: Open a specific course's details ----
    try {
      const cBody = JSON.parse(coursesRes.body);
      const cList = cBody?.data || cBody;
      if (Array.isArray(cList) && cList.length > 0) {
        const courseId = cList[Math.floor(Math.random() * cList.length)].id;
        const courseDetailRes = http.get(`${BASE_URL}/api/courses/${courseId}`, {
          tags: { name: 'course_detail' },
        });
        const courseOk = check(courseDetailRes, {
          'course detail status is 200': (r) => r.status === 200,
        });
        apiFailRate.add(!courseOk);
      }
    } catch (e) {
      apiFailRate.add(true);
    }
  }

  sleep(randomThinkTime(2, 5)); // simulate reading/thinking before the next loop
}

function randomThinkTime(min, max) {
  return Math.random() * (max - min) + min;
}
