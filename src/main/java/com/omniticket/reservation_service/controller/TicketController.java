package com.omniticket.reservation_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.omniticket.reservation_service.dto.TicketRequestDTO;
import com.omniticket.reservation_service.dto.TicketResponseDTO;
import com.omniticket.reservation_service.service.TicketService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<Page<TicketResponseDTO>> getAllTickets(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ticketService.getAllTickets(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody TicketRequestDTO ticketRequestDTO) {
        return ResponseEntity.ok(ticketService.createTicket(ticketRequestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> updateTicket(@PathVariable Long id,
            @Valid @RequestBody TicketRequestDTO ticketDetails) {
        return ResponseEntity.ok(ticketService.updateTicket(id, ticketDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<TicketResponseDTO> reserveTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reserveTicket(id));
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<TicketResponseDTO> purchase(@PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(ticketService.purchaseTicket(id, idempotencyKey));
    }

}
