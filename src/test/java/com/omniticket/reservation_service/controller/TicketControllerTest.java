package com.omniticket.reservation_service.controller;

import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private Ticket createTicket(Long id, String seatNumber, Double price, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setSeatNumber(seatNumber);
        ticket.setPrice(price);
        ticket.setStatus(status);
        ticket.setReservedAt(status == TicketStatus.RESERVED ? LocalDateTime.now() : null);
        return ticket;
    }

    @Test
    void givenAvailableTicket_whenGetAllTickets_thenReturnTicketList() throws Exception {
        List<Ticket> tickets = List.of(
                createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE),
                createTicket(2L, "A2", 150.0, TicketStatus.AVAILABLE));

        when(ticketService.getAllTickets()).thenReturn(tickets);

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[1].seatNumber").value("A2"));
    }

    @Test
    void givenExistingTicketId_whenGetTicket_thenReturnTicket() throws Exception {
        Ticket ticket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);

        when(ticketService.getTicket(1L)).thenReturn(ticket);

        mockMvc.perform(get("/api/v1/tickets/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.seatNumber").value("A1"))
                .andExpect(jsonPath("$.price").value(100.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void givenNonExistingTicketId_whenGetTicket_thenThrowException() throws Exception {
        when(ticketService.getTicket(999L)).thenThrow(new ResourceNotFoundException("Ticket not found with id: 999"));

        mockMvc.perform(get("/api/v1/tickets/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenValidTicket_whenCreateTicket_thenReturnsCreatedTicket() throws Exception {
        Ticket ticket = createTicket(null, "B1", 200.0, TicketStatus.AVAILABLE);
        Ticket savedTicket = createTicket(1L, "B1", 200.0, TicketStatus.AVAILABLE);

        when(ticketService.createTicket(any(Ticket.class))).thenReturn(savedTicket);

        mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatNumber\":\"B1\",\"price\":200.0,\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.seatNumber").value("B1"))
                .andExpect(jsonPath("$.price").value(200.0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void givenExistingTicket_whenUpdateTicket_thenReturnsUpdatedTicket() throws Exception {
        Ticket updatedTicket = createTicket(1L, "C1", 250.0, TicketStatus.AVAILABLE);

        when(ticketService.updateTicket(eq(1L), any(Ticket.class))).thenReturn(updatedTicket);

        mockMvc.perform(put("/api/v1/tickets/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatNumber\":\"C1\",\"price\":250.0,\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatNumber").value("C1"))
                .andExpect(jsonPath("$.price").value(250.0));
    }

    @Test
    void givenExistingTicket_whenDeleteTicket_thenReturnsNoContent() throws Exception {
        doNothing().when(ticketService).deleteTicket(1L);

        mockMvc.perform(delete("/api/v1/tickets/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenAvailableTicket_whenReserveTicket_thenReturnsReservedTicket() throws Exception {
        Ticket reservedTicket = createTicket(1L, "A1", 100.0, TicketStatus.RESERVED);

        when(ticketService.reserveTicket(1L)).thenReturn(reservedTicket);

        mockMvc.perform(post("/api/v1/tickets/{id}/reserve", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void givenReservedTicket_whenPurchase_thenReturnsSoldTicket() throws Exception {
        Ticket soldTicket = createTicket(1L, "A1", 100.0, TicketStatus.SOLD);

        when(ticketService.purchaseTicket(1L)).thenReturn(soldTicket);

        mockMvc.perform(post("/api/v1/tickets/{id}/purchase", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    void givenSoldTicket_whenPurchase_thenThrowsException() throws Exception {
        when(ticketService.purchaseTicket(1L))
                .thenThrow(new RuntimeException("Satın almak için önce rezervasyon yapmalısınız!"));

        mockMvc.perform(post("/api/v1/tickets/{id}/purchase", 1L))
                .andExpect(status().isInternalServerError());
    }
}