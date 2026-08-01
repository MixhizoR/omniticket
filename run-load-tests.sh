#!/bin/bash
set -e

RESULT_FILE="LOAD_TEST_RESULTS.md"
VU_LEVELS="100 500 1000"

echo "🚀 Starting OmniTicket Load Test Orchestration..."
echo ""

echo "🔨 Building the application..."
./mvnw clean package -DskipTests > /dev/null 2>&1

echo "# 🚀 OmniTicket Load Test Results" > $RESULT_FILE
echo "**Test Date:** $(date)" >> $RESULT_FILE
echo "" >> $RESULT_FILE
echo "These tests were executed automatically via \`run-load-tests.sh\` using k6. The system was containerized using Docker Compose (PostgreSQL, Redis, RabbitMQ)." >> $RESULT_FILE
echo "" >> $RESULT_FILE
echo "Each test was run at three concurrency levels: **100, 500, and 1000 virtual users (VU)**. Each run consists of a 30s ramp-up, 1m sustained load, and 30s ramp-down (2 minutes total per run)." >> $RESULT_FILE
echo "" >> $RESULT_FILE

start_env() {
    echo "🧹 Cleaning up old environment..."
    docker compose down -v > /dev/null 2>&1

    echo "🐳 Starting Infrastructure (Postgres, Redis, RabbitMQ)..."
    docker compose up -d postgres redis rabbitmq > /dev/null 2>&1
    echo "⏳ Waiting for infrastructure to be healthy..."
    docker compose up -d --wait postgres redis rabbitmq > /dev/null 2>&1

    echo "☕ Starting Spring Boot Application..."
    java -jar target/*.jar > app.log 2>&1 &
    APP_PID=$!

    echo "⏳ Waiting for Application to start (this may take a few seconds)..."
    until curl -s -f -o /dev/null "http://localhost:8080/api/v1/tickets"; do
        sleep 2
    done
    echo "✅ Application is up and running!"
}

stop_env() {
    echo "🧹 Stopping application and cleaning containers..."
    kill $APP_PID > /dev/null 2>&1 || true
    wait $APP_PID > /dev/null 2>&1 || true
    docker compose down > /dev/null 2>&1
}

run_test() {
    local test_file=$1
    local test_title=$2
    local vus=$3
    
    echo "=================================================="
    echo "🏃 Preparing for: $test_title (${vus} VU)"
    start_env
    
    echo "🏃 Running $test_title (${vus} VU)..."
    
    echo "## $test_title — ${vus} VU" >> $RESULT_FILE
    echo "" >> $RESULT_FILE
    echo '```text' >> $RESULT_FILE
    
    local k6_exit=0
    docker run --pull=never --rm -i --network="host" \
        -e K6_NO_COLOR=true -e VUS=$vus \
        grafana/k6 run --quiet - < $test_file >> $RESULT_FILE 2>&1 \
        || k6_exit=$?
    
    echo '```' >> $RESULT_FILE
    echo "" >> $RESULT_FILE
    
    if [ $k6_exit -eq 0 ]; then
        echo "**✅ Result: PASS**" >> $RESULT_FILE
    else
        echo "**❌ Result: FAIL (k6 exit code: $k6_exit)**" >> $RESULT_FILE
    fi
    echo "" >> $RESULT_FILE
    echo "---" >> $RESULT_FILE
    echo ""
    
    stop_env
}

# TEST 1: Baseline Read (GET) — 100, 500, 1000 VU
for vus in $VU_LEVELS; do
    run_test "src/test/load/1-baseline-read-test.js" "1. Baseline Read Test (GET /tickets)" $vus
done

# TEST 2: Concurrency Reservation (POST /reserve) — 100, 500, 1000 VU
for vus in $VU_LEVELS; do
    run_test "src/test/load/2-concurrency-reservation-test.js" "2. Concurrency Reservation Test (POST /reserve)" $vus
done

# TEST 3: Idempotency Purchase (POST /purchase) — 50 VU (functional test, not benchmark)
run_test "src/test/load/3-idempotency-purchase-test.js" "3. Idempotency Purchase Test (POST /purchase)" 50

echo ""
echo "✅ All tests completed! Results are saved in $RESULT_FILE"