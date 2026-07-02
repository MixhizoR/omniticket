package com.omniticket.reservation_service.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketScheduler {

    private final TicketRepository ticketRepository;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void releaseExpiredTickets() {
        log.info("Checking for expired tickets..." + LocalDateTime.now(java.time.ZoneOffset.UTC));

        LocalDateTime oneMinuteAgo = LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1);

        List<Ticket> reservedTickets = ticketRepository.findAllByStatusAndReservedAtBefore(
                TicketStatus.RESERVED, oneMinuteAgo);

        for (Ticket ticket : reservedTickets) {
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setReservedAt(null);
            ticketRepository.save(ticket);
            log.info("Released reserved ticket: {}", ticket.getId());
        }
    }
}
