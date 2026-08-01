# 🚀 OmniTicket Load Test Results
**Test Date:** Cts 01 Ağu 2026 12:14:06 +03

These tests were executed automatically via `run-load-tests.sh` using k6. The system was containerized using Docker Compose (PostgreSQL, Redis, RabbitMQ).

Each test was run at three concurrency levels: **100, 500, and 1000 virtual users (VU)**. Each run consists of a 30s ramp-up, 1m sustained load, and 30s ramp-down (2 minutes total per run).

## 1. Baseline Read Test (GET /tickets) — 100 VU

```text


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<200' p(95)=3ms
    ✓ 'p(99)<1000' p(99)=4.71ms


  █ TOTAL RESULTS 

    checks_total.......: 9045    74.969621/s
    checks_succeeded...: 100.00% 9045 out of 9045
    checks_failed......: 0.00%   0 out of 9045

    ✓ status was 200

    HTTP
    http_req_duration..............: avg=1.68ms min=512.29µs med=1.56ms max=12.89ms p(90)=2.33ms p(95)=3ms
      { expected_response:true }...: avg=1.68ms min=512.29µs med=1.56ms max=12.89ms p(90)=2.33ms p(95)=3ms
    http_req_failed................: 0.00%  0 out of 9045
    http_reqs......................: 9045   74.969621/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s       med=1s     max=1.01s   p(90)=1s     p(95)=1s 
    iterations.....................: 9045   74.969621/s
    vus............................: 2      min=2         max=100
    vus_max........................: 100    min=100       max=100

    NETWORK
    data_received..................: 8.1 MB 67 kB/s
    data_sent......................: 896 kB 7.4 kB/s



```

**✅ Result: PASS**

---
## 1. Baseline Read Test (GET /tickets) — 500 VU

```text


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<200' p(95)=2.07ms
    ✓ 'p(99)<1000' p(99)=3.94ms


  █ TOTAL RESULTS 

    checks_total.......: 45171   374.648851/s
    checks_succeeded...: 100.00% 45171 out of 45171
    checks_failed......: 0.00%   0 out of 45171

    ✓ status was 200

    HTTP
    http_req_duration..............: avg=1.36ms min=413.18µs med=1.26ms max=18.58ms p(90)=1.78ms p(95)=2.07ms
      { expected_response:true }...: avg=1.36ms min=413.18µs med=1.26ms max=18.58ms p(90)=1.78ms p(95)=2.07ms
    http_req_failed................: 0.00%  0 out of 45171
    http_reqs......................: 45171  374.648851/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s       med=1s     max=1.02s   p(90)=1s     p(95)=1s    
    iterations.....................: 45171  374.648851/s
    vus............................: 9      min=9          max=500
    vus_max........................: 500    min=500        max=500

    NETWORK
    data_received..................: 41 MB  337 kB/s
    data_sent......................: 4.5 MB 37 kB/s



```

**✅ Result: PASS**

---
## 1. Baseline Read Test (GET /tickets) — 1000 VU

```text


  █ THRESHOLDS 

    http_req_duration
    ✓ 'p(95)<200' p(95)=2.11ms
    ✓ 'p(99)<1000' p(99)=3.67ms


  █ TOTAL RESULTS 

    checks_total.......: 90351   749.155821/s
    checks_succeeded...: 100.00% 90351 out of 90351
    checks_failed......: 0.00%   0 out of 90351

    ✓ status was 200

    HTTP
    http_req_duration..............: avg=1.31ms min=432.78µs med=1.2ms max=16.54ms p(90)=1.82ms p(95)=2.11ms
      { expected_response:true }...: avg=1.31ms min=432.78µs med=1.2ms max=16.54ms p(90)=1.82ms p(95)=2.11ms
    http_req_failed................: 0.00%  0 out of 90351
    http_reqs......................: 90351  749.155821/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s       med=1s    max=1.01s   p(90)=1s     p(95)=1s    
    iterations.....................: 90351  749.155821/s
    vus............................: 19     min=19         max=1000
    vus_max........................: 1000   min=1000       max=1000

    NETWORK
    data_received..................: 81 MB  673 kB/s
    data_sent......................: 8.9 MB 74 kB/s



```

**✅ Result: PASS**

---
## 2. Concurrency Reservation Test (POST /reserve) — 100 VU

```text


  █ THRESHOLDS 

    checks
    ✓ 'rate>0.99' rate=100.00%

    http_req_duration{expected_response:true}
    ✓ 'p(95)<500' p(95)=3.94ms
    ✓ 'p(99)<2000' p(99)=5.87ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 9040    74.898059/s
    checks_succeeded...: 100.00% 9040 out of 9040
    checks_failed......: 0.00%   0 out of 9040

    ✓ reserve status is valid (no 500)

    HTTP
    http_req_duration..............: avg=2.15ms min=631.35µs med=1.93ms max=37.75ms p(90)=3.16ms p(95)=3.94ms
      { expected_response:true }...: avg=2.15ms min=631.35µs med=1.93ms max=37.75ms p(90)=3.16ms p(95)=3.94ms
    http_req_failed................: 0.00%  0 out of 9040
    http_reqs......................: 9040   74.898059/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s       med=1s     max=1.03s   p(90)=1s     p(95)=1s    
    iterations.....................: 9040   74.898059/s
    vus............................: 3      min=3         max=100
    vus_max........................: 100    min=100       max=100

    NETWORK
    data_received..................: 1.6 MB 13 kB/s
    data_sent......................: 1.0 MB 8.5 kB/s



```

**✅ Result: PASS**

---
## 2. Concurrency Reservation Test (POST /reserve) — 500 VU

```text


  █ THRESHOLDS 

    checks
    ✓ 'rate>0.99' rate=100.00%

    http_req_duration{expected_response:true}
    ✓ 'p(95)<500' p(95)=3.35ms
    ✓ 'p(99)<2000' p(99)=5.18ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 45157   374.452264/s
    checks_succeeded...: 100.00% 45157 out of 45157
    checks_failed......: 0.00%   0 out of 45157

    ✓ reserve status is valid (no 500)

    HTTP
    http_req_duration..............: avg=1.75ms min=535.35µs med=1.52ms max=29.45ms p(90)=2.76ms p(95)=3.35ms
      { expected_response:true }...: avg=1.75ms min=535.35µs med=1.52ms max=29.45ms p(90)=2.76ms p(95)=3.35ms
    http_req_failed................: 0.00%  0 out of 45157
    http_reqs......................: 45157  374.452264/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s       med=1s     max=1.03s   p(90)=1s     p(95)=1s    
    iterations.....................: 45157  374.452264/s
    vus............................: 9      min=9          max=500
    vus_max........................: 500    min=500        max=500

    NETWORK
    data_received..................: 7.9 MB 66 kB/s
    data_sent......................: 5.1 MB 43 kB/s



```

**✅ Result: PASS**

---
## 2. Concurrency Reservation Test (POST /reserve) — 1000 VU

```text


  █ THRESHOLDS 

    checks
    ✓ 'rate>0.99' rate=100.00%

    http_req_duration{expected_response:true}
    ✓ 'p(95)<500' p(95)=3.76ms
    ✓ 'p(99)<2000' p(99)=5.73ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 90308   748.539838/s
    checks_succeeded...: 100.00% 90308 out of 90308
    checks_failed......: 0.00%   0 out of 90308

    ✓ reserve status is valid (no 500)

    HTTP
    http_req_duration..............: avg=1.84ms min=580.88µs med=1.54ms max=32.82ms p(90)=3.03ms p(95)=3.76ms
      { expected_response:true }...: avg=1.84ms min=580.88µs med=1.54ms max=32.82ms p(90)=3.03ms p(95)=3.76ms
    http_req_failed................: 0.00% 0 out of 90308
    http_reqs......................: 90308 748.539838/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s       med=1s     max=1.03s   p(90)=1s     p(95)=1s    
    iterations.....................: 90308 748.539838/s
    vus............................: 18    min=18         max=1000
    vus_max........................: 1000  min=1000       max=1000

    NETWORK
    data_received..................: 16 MB 131 kB/s
    data_sent......................: 10 MB 85 kB/s



```

**✅ Result: PASS**

---
## 3. Idempotency Purchase Test (POST /purchase) — 50 VU

```text


  █ THRESHOLDS 

    checks
    ✓ 'rate>0.99' rate=100.00%

    http_req_duration
    ✓ 'p(99)<2000' p(99)=6.49ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS 

    checks_total.......: 4540    37.674205/s
    checks_succeeded...: 100.00% 4540 out of 4540
    checks_failed......: 0.00%   0 out of 4540

    ✓ reserve valid
    ✓ purchase valid
    ✓ idempotent returns 200
    ✓ same ticket id returned

    HTTP
    http_req_duration..............: avg=2.57ms min=1.01ms med=2.25ms max=39.82ms p(90)=3.74ms p(95)=4.5ms
      { expected_response:true }...: avg=2.57ms min=1.01ms med=2.25ms max=39.82ms p(90)=3.74ms p(95)=4.5ms
    http_req_failed................: 0.00%  0 out of 4535
    http_reqs......................: 4535   37.632714/s

    EXECUTION
    iteration_duration.............: avg=1s     min=1s     med=1s     max=1.07s   p(90)=1s     p(95)=1s   
    iterations.....................: 4525   37.549731/s
    vus............................: 2      min=2         max=50
    vus_max........................: 50     min=50        max=50

    NETWORK
    data_received..................: 795 kB 6.6 kB/s
    data_sent......................: 517 kB 4.3 kB/s



```

**✅ Result: PASS**

---
