package com.omniticket.reservation_service.common.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.omniticket.reservation_service.exception.TicketLockAcquisitionException;
import com.omniticket.reservation_service.exception.TicketSystemException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockTemplate {
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    public <T> T executeWithLock(String lockKey, Supplier<T> databaseAction) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            // leaseTime is explicit (not -1) to disable the Redisson watchdog.
            // A fixed lease guarantees the lock is released even if the holder dies,
            // and avoids background renewal traffic (EVALSHA) overwhelming Redis under
            // load.
            // waitTime=2s is a deliberate fail-fast: under lock contention a losing
            // request should return 503 quickly instead of hanging in a queue for 5s.
            if (!lock.tryLock(2, 30, TimeUnit.SECONDS)) {
                throw new TicketLockAcquisitionException("System is busy, please try again!");
            }
            locked = true;
            log.info("Lock acquired: {}", lockKey);

            return transactionTemplate.execute(status -> databaseAction.get());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketSystemException("A system error occurred.", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Operation completed, lock released: {}", lockKey);
            }
        }
    }
}
