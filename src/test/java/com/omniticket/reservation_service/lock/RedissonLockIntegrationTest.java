package com.omniticket.reservation_service.lock;

import com.omniticket.reservation_service.BaseIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class RedissonLockIntegrationTest extends BaseIntegrationTest {

    private static RedissonClient redissonClient;

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
        redissonClient = Redisson.create(config);
    }

    @AfterAll
    static void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    void givenRedisAvailable_whenAcquireLock_thenLockIsAcquired() {
        RLock lock = redissonClient.getLock("test-lock-1");

        boolean acquired = lock.tryLock();
        assertTrue(acquired, "Lock should be acquired");

        assertTrue(lock.isLocked());
        assertTrue(lock.isHeldByCurrentThread());

        lock.unlock();
        assertFalse(lock.isLocked());
    }

    @Test
    void givenLockHeld_whenMultipleThreadsTryLock_thenOnlyOneAcquires() throws InterruptedException {
        int numberOfThreads = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.execute(() -> {
                try {
                    latch.await();
                    RLock threadLock = redissonClient.getLock("test-lock-2");
                    if (threadLock.tryLock(1, 5, TimeUnit.SECONDS)) {
                        try {
                            successCount.incrementAndGet();
                            Thread.sleep(100);
                        } finally {
                            threadLock.unlock();
                        }
                    }
                } catch (Exception e) {
                    // Lock acquisition failure is expected
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(),
                "Only one thread should acquire the lock at a time");
    }

    @Test
    void givenLockedResource_whenUnlock_thenOtherThreadsCanProceed() throws InterruptedException {
        RLock lock = redissonClient.getLock("test-lock-3");

        lock.lock();
        assertTrue(lock.isLocked());

        AtomicInteger secondAcquirer = new AtomicInteger(0);
        CountDownLatch threadStarted = new CountDownLatch(1);

        Thread t2 = new Thread(() -> {
            RLock otherLock = redissonClient.getLock("test-lock-3");
            threadStarted.countDown();
            // Busy-wait trying to acquire the lock
            while (true) {
                if (otherLock.tryLock()) {
                    secondAcquirer.incrementAndGet();
                    otherLock.unlock();
                    break;
                }
            }
        });

        t2.start();
        threadStarted.await(2, TimeUnit.SECONDS);

        // Give the thread a moment to attempt and fail
        await().atMost(1, TimeUnit.SECONDS).until(() -> secondAcquirer.get() == 0);
        assertEquals(0, secondAcquirer.get());

        // Now unlock so the second thread can proceed
        lock.unlock();

        await().atMost(2, TimeUnit.SECONDS).until(() -> secondAcquirer.get() == 1);
        assertEquals(1, secondAcquirer.get(),
                "Second thread should acquire lock after first unlocks");
    }

    @Test
    void givenLockTimeout_whenWaitExceeds_thenLockNotAcquired() throws InterruptedException {
        RLock lock = redissonClient.getLock("test-lock-4");
        lock.lock();

        AtomicInteger acquired = new AtomicInteger(0);

        Thread t2 = new Thread(() -> {
            RLock otherLock = redissonClient.getLock("test-lock-4");
            try {
                if (otherLock.tryLock(1, 3, TimeUnit.SECONDS)) {
                    acquired.incrementAndGet();
                    otherLock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t2.start();

        // Wait for the timeout attempt to complete
        await().atMost(5, TimeUnit.SECONDS).until(() -> !t2.isAlive());

        assertEquals(0, acquired.get(),
                "Lock should not be acquired when wait time is exceeded");

        lock.unlock();
    }
}