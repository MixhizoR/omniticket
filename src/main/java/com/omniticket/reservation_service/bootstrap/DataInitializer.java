package com.omniticket.reservation_service.bootstrap;

import java.math.BigDecimal;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final TicketRepository ticketRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (ticketRepository.count() == 0) {
            log.info("Database is empty, creating sample tickets... 🎟️");

            for (int i = 1; i <= 5; i++) {
                Ticket ticket = new Ticket();
                ticket.setSeatNumber("Row-A Seat-" + i);
                ticket.setPrice(BigDecimal.valueOf(150.0 * i));
                ticket.setStatus(TicketStatus.AVAILABLE);

                ticketRepository.save(ticket);
            }

            log.info("Success! 5 tickets loaded into the system. ✅");
        } else {
            log.info("Tickets already exist in the database, no new data added. ✨");
        }
    }
}