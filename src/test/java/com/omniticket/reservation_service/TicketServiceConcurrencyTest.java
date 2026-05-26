package com.omniticket.reservation_service;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import com.omniticket.reservation_service.service.TicketService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TicketServiceConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    void givenAvailableTicket_when100ThreadsReserveSimultaneously_thenExactlyOneSuccess() throws InterruptedException {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber("CONCUR-100");
        ticket.setPrice(100.0);
        ticket.setStatus(TicketStatus.AVAILABLE);
        Ticket savedTicket = ticketRepository.save(ticket);

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await();
                    ticketService.reserveTicket(savedTicket.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(1, successCount.get(),
                "Only 1 thread should succeed in reserving the ticket out of 100");
        assertEquals(99, failCount.get(),
                "99 threads should fail to reserve the ticket");
    }

    @Test
    void givenReservedTicket_whenConcurrentPurchaseAttempts_thenFirstSucceedsRestFail() throws InterruptedException {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber("CONCUR-PURCHASE");
        ticket.setPrice(150.0);
        ticket.setStatus(TicketStatus.AVAILABLE);
        Ticket savedTicket = ticketRepository.save(ticket);
        ticketService.reserveTicket(savedTicket.getId());

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await();
                    ticketService.purchaseTicket(savedTicket.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        Ticket finalTicket = ticketRepository.findById(savedTicket.getId()).orElse(null);
        assertEquals(TicketStatus.SOLD, finalTicket.getStatus(),
                "Ticket should be SOLD after any purchase succeeds");
    }

    @Test
    void givenHighConcurrency_whenMultipleTickets_thenAllTicketsReservedOnceEach() throws InterruptedException {
        int ticketCount = 5;
        int threadsPerTicket = 20;

        Ticket[] savedTickets = new Ticket[ticketCount];
        for (int i = 0; i < ticketCount; i++) {
            Ticket ticket = new Ticket();
            ticket.setSeatNumber("MULTI-" + (i + 1));
            ticket.setPrice(100.0 * (i + 1));
            ticket.setStatus(TicketStatus.AVAILABLE);
            savedTickets[i] = ticketRepository.save(ticket);
        }

        int totalThreads = ticketCount * threadsPerTicket;
        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalFail = new AtomicInteger(0);

        for (int t = 0; t < ticketCount; t++) {
            Long ticketId = savedTickets[t].getId();
            for (int i = 0; i < threadsPerTicket; i++) {
                executorService.execute(() -> {
                    try {
                        latch.await();
                        ticketService.reserveTicket(ticketId);
                        totalSuccess.incrementAndGet();
                    } catch (Exception e) {
                        totalFail.incrementAndGet();
                    }
                });
            }
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(ticketCount, totalSuccess.get(),
                "Each ticket should be reserved exactly once");
        assertEquals(totalThreads - ticketCount, totalFail.get(),
                "All other attempts should fail");
    }

    @Test
    void shouldOnlyOneUserReserveTicketWhenMultipleUsersTryAtOnce() throws InterruptedException {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber("TEST-101");
        ticket.setPrice(100.0);
        ticket.setStatus(TicketStatus.AVAILABLE);
        Ticket savedTicket = ticketRepository.save(ticket);

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await();
                    ticketService.reserveTicket(savedTicket.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(1, successCount.get(),
                "Sadece 1 kişi bilet alabilmeliydi!");
        assertEquals(9, failCount.get(),
                "9 kişi biletin dolu olduğu hatasını almalıydı!");
    }
}