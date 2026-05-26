package com.omniticket.reservation_service.scheduler;

import com.omniticket.reservation_service.BaseIntegrationTest;
import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketSchedulerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TicketScheduler ticketScheduler;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    private Ticket createTicket(String seatNumber, Double price, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(seatNumber);
        ticket.setPrice(price);
        ticket.setStatus(status);
        return ticket;
    }

    @Test
    void givenExpiredReservation_whenSchedulerRuns_thenTicketReleased() {
        Ticket expiredReserved = createTicket("A1", 100.0, TicketStatus.RESERVED);
        expiredReserved.setReservedAt(LocalDateTime.now().minusMinutes(2));
        ticketRepository.save(expiredReserved);

        ticketScheduler.releaseExpiredTickets();

        List<Ticket> tickets = ticketRepository.findAllByOrderByIdAsc();
        assertEquals(1, tickets.size());
        assertEquals(TicketStatus.AVAILABLE, tickets.get(0).getStatus());
        assertNull(tickets.get(0).getReservedAt());
    }

    @Test
    void givenFreshReservation_whenSchedulerRuns_thenTicketNotReleased() {
        Ticket freshReserved = createTicket("B2", 150.0, TicketStatus.RESERVED);
        freshReserved.setReservedAt(LocalDateTime.now());
        ticketRepository.save(freshReserved);

        ticketScheduler.releaseExpiredTickets();

        List<Ticket> tickets = ticketRepository.findAllByOrderByIdAsc();
        assertEquals(1, tickets.size());
        assertEquals(TicketStatus.RESERVED, tickets.get(0).getStatus());
        assertNotNull(tickets.get(0).getReservedAt());
    }

    @Test
    void givenNoExpiredReservations_whenSchedulerRuns_thenNoChanges() {
        Ticket availableTicket = createTicket("C3", 200.0, TicketStatus.AVAILABLE);
        Ticket soldTicket = createTicket("D4", 250.0, TicketStatus.SOLD);
        ticketRepository.save(availableTicket);
        ticketRepository.save(soldTicket);

        ticketScheduler.releaseExpiredTickets();

        List<Ticket> tickets = ticketRepository.findAllByOrderByIdAsc();
        assertEquals(2, tickets.size());
        assertEquals(TicketStatus.AVAILABLE, tickets.get(0).getStatus());
        assertEquals(TicketStatus.SOLD, tickets.get(1).getStatus());
    }
}