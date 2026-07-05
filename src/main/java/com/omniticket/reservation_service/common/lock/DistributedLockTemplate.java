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
            if (!lock.tryLock(5, -1, TimeUnit.SECONDS)) {
                throw new TicketLockAcquisitionException("Şu an çok yoğun, lütfen tekrar deneyin!");
            }
            locked = true;
            log.info("Kilit alındı: {}", lockKey);

            return transactionTemplate.execute(status -> databaseAction.get());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketSystemException("Sistemsel bir hata oluştu.", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("İşlem bitti, kilit açıldı: {}", lockKey);
            }
        }
    }
}
