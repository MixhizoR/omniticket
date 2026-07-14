package com.omniticket.reservation_service.repository;

import com.omniticket.reservation_service.AbstractBaseIntegrationTest;
import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TicketRepositoryIntegrationTest extends AbstractBaseIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    private Ticket createTicket(String seatNumber, BigDecimal price, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(seatNumber);
        ticket.setPrice(price);
        ticket.setStatus(status);
        if (status == TicketStatus.RESERVED) {
            ticket.setReservedAt(LocalDateTime.now());
        }
        return ticket;
    }

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    void givenTicket_whenSave_thenPersistsAndReturnsTicket() {
        Ticket ticket = createTicket("A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        Ticket savedTicket = ticketRepository.save(ticket);

        assertNotNull(savedTicket.getId());
        assertEquals("A1", savedTicket.getSeatNumber());
        assertEquals(BigDecimal.valueOf(100.0), savedTicket.getPrice());
        assertEquals(TicketStatus.AVAILABLE, savedTicket.getStatus());
    }

    @Test
    void givenPersistedTicket_whenFindById_thenReturnsTicket() {
        Ticket ticket = createTicket("A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);
        Ticket savedTicket = ticketRepository.save(ticket);

        Optional<Ticket> foundTicket = ticketRepository.findById(savedTicket.getId());

        assertTrue(foundTicket.isPresent());
        assertEquals(savedTicket.getId(), foundTicket.get().getId());
        assertEquals("A1", foundTicket.get().getSeatNumber());
    }

    @Test
    void givenMultipleTickets_whenFindAllByOrder_thenReturnsOrderedAsc() {
        Ticket ticket1 = ticketRepository.save(createTicket("B2", BigDecimal.valueOf(150.0), TicketStatus.AVAILABLE));
        Ticket ticket2 = ticketRepository.save(createTicket("A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE));
        Ticket ticket3 = ticketRepository.save(createTicket("C3", BigDecimal.valueOf(200.0), TicketStatus.AVAILABLE));

        List<Ticket> tickets = ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        assertEquals(3, tickets.size());
        assertEquals(ticket1.getId(), tickets.get(0).getId());
        assertEquals(ticket2.getId(), tickets.get(1).getId());
        assertEquals(ticket3.getId(), tickets.get(2).getId());
    }

    @Test
    void givenReservedTicketBeforeThreshold_whenFindAllByStatusAndTime_thenReturnsTicket() {
        Ticket expiredReserved = createTicket("A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        expiredReserved.setReservedAt(LocalDateTime.now().minusMinutes(2));
        ticketRepository.save(expiredReserved);

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<Ticket> expiredTickets = ticketRepository.findAllByStatusAndReservedAtBefore(
                TicketStatus.RESERVED, oneMinuteAgo, PageRequest.of(0, 100));

        assertEquals(1, expiredTickets.size());
        assertEquals("A1", expiredTickets.get(0).getSeatNumber());
    }

    @Test
    void givenSoldTicket_whenFindAllByStatus_thenExcludesSoldTickets() {
        ticketRepository.save(createTicket("A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD));
        ticketRepository.save(createTicket("A2", BigDecimal.valueOf(150.0), TicketStatus.RESERVED));
        ticketRepository.save(createTicket("A3", BigDecimal.valueOf(200.0), TicketStatus.AVAILABLE));

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<Ticket> result = ticketRepository.findAllByStatusAndReservedAtBefore(
                TicketStatus.RESERVED, oneMinuteAgo, PageRequest.of(0, 100));

        assertTrue(result.isEmpty());
    }
}