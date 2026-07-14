package com.omniticket.reservation_service.scheduler;

import com.omniticket.reservation_service.AbstractBaseIntegrationTest;
import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketSchedulerIntegrationTest extends AbstractBaseIntegrationTest {

    @Autowired
    private TicketScheduler ticketScheduler;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    private Ticket createTicket(String seatNumber, BigDecimal price, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(seatNumber);
        ticket.setPrice(price);
        ticket.setStatus(status);
        return ticket;
    }

    @Test
    void givenExpiredReservation_whenSchedulerRuns_thenTicketReleased() {
        Ticket expiredReserved = createTicket("A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        expiredReserved.setReservedAt(LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(2));
        ticketRepository.save(expiredReserved);

        ticketScheduler.releaseExpiredTickets();

        List<Ticket> tickets = ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        assertEquals(1, tickets.size());
        assertEquals(TicketStatus.AVAILABLE, tickets.get(0).getStatus());
        assertNull(tickets.get(0).getReservedAt());
    }

    @Test
    void givenFreshReservation_whenSchedulerRuns_thenTicketNotReleased() {
        Ticket freshReserved = createTicket("B2", BigDecimal.valueOf(150.0), TicketStatus.RESERVED);
        freshReserved.setReservedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        ticketRepository.save(freshReserved);

        ticketScheduler.releaseExpiredTickets();

        List<Ticket> tickets = ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        assertEquals(1, tickets.size());
        assertEquals(TicketStatus.RESERVED, tickets.get(0).getStatus());
        assertNotNull(tickets.get(0).getReservedAt());
    }

    @Test
    void givenNoExpiredReservations_whenSchedulerRuns_thenNoChanges() {
        Ticket availableTicket = createTicket("C3", BigDecimal.valueOf(200.0), TicketStatus.AVAILABLE);
        Ticket soldTicket = createTicket("D4", BigDecimal.valueOf(250.0), TicketStatus.SOLD);
        ticketRepository.save(availableTicket);
        ticketRepository.save(soldTicket);

        ticketScheduler.releaseExpiredTickets();

        List<Ticket> tickets = ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        assertEquals(2, tickets.size());
        assertEquals(TicketStatus.AVAILABLE, tickets.get(0).getStatus());
        assertEquals(TicketStatus.SOLD, tickets.get(1).getStatus());
    }
}