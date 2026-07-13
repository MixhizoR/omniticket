package com.omniticket.reservation_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByStatusAndReservedAtBefore(TicketStatus status, LocalDateTime time, Pageable pageable);
}
