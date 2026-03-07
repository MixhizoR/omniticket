package com.omniticket.reservation_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByOrderByIdAsc();

    List<Ticket> findAllByStatusAndReservedAtBefore(TicketStatus status, LocalDateTime time);
}
