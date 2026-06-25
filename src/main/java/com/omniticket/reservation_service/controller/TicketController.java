package com.omniticket.reservation_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.service.TicketService;
import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody Ticket ticket) {
        return ResponseEntity.ok(ticketService.createTicket(ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable Long id, @RequestBody Ticket ticketDetails) {
        return ResponseEntity.ok(ticketService.updateTicket(id, ticketDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Ticket> reserveTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reserveTicket(id));
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<Ticket> purchase(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.purchaseTicket(id));
    }

    // TEST İÇİN: Süresi dolmuş bir rezervasyon oluşturur
    @PostMapping("/test/create-expired")
    public ResponseEntity<Ticket> createExpiredTicket() {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber("T101");
        ticket.setPrice(100.0);
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setReservedAt(LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(2));
        return ResponseEntity.ok(ticketService.createTicket(ticket));
    }

}
