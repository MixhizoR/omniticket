package com.omniticket.reservation_service.controller;

import com.omniticket.reservation_service.dto.TicketResponseDTO;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private TicketService ticketService;

        private TicketResponseDTO createResponseDTO(Long id, String seatNumber, BigDecimal price, TicketStatus status,
                        LocalDateTime reservedAt) {
                return new TicketResponseDTO(id, seatNumber, price, status, reservedAt);
        }

        @Test
        void givenAvailableTicket_whenGetAllTickets_thenReturnTicketList() throws Exception {
                List<TicketResponseDTO> tickets = List.of(
                                createResponseDTO(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE, null),
                                createResponseDTO(2L, "A2", BigDecimal.valueOf(150.0), TicketStatus.AVAILABLE, null));

                when(ticketService.getAllTickets()).thenReturn(tickets);

                mockMvc.perform(get("/api/v1/tickets"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.size()").value(2))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[1].seatNumber").value("A2"));
        }

        @Test
        void givenExistingTicketId_whenGetTicket_thenReturnTicket() throws Exception {
                TicketResponseDTO ticket = createResponseDTO(1L, "A1", BigDecimal.valueOf(100.0),
                                TicketStatus.AVAILABLE, null);

                when(ticketService.getTicket(1L)).thenReturn(ticket);

                mockMvc.perform(get("/api/v1/tickets/{id}", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.seatNumber").value("A1"))
                                .andExpect(jsonPath("$.price").value(100.0))
                                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        void givenNonExistingTicketId_whenGetTicket_thenThrowException() throws Exception {
                when(ticketService.getTicket(999L))
                                .thenThrow(new ResourceNotFoundException("Ticket not found with id: 999"));

                mockMvc.perform(get("/api/v1/tickets/{id}", 999L))
                                .andExpect(status().isNotFound());
        }

        @Test
        void givenValidTicket_whenCreateTicket_thenReturnsCreatedTicket() throws Exception {
                TicketResponseDTO savedTicket = createResponseDTO(3L, "B1", BigDecimal.valueOf(200.0),
                                TicketStatus.AVAILABLE, null);

                when(ticketService.createTicket(any())).thenReturn(savedTicket);

                mockMvc.perform(post("/api/v1/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatNumber\":\"B1\",\"price\":200.0,\"status\":\"AVAILABLE\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(3))
                                .andExpect(jsonPath("$.seatNumber").value("B1"))
                                .andExpect(jsonPath("$.price").value(200.0))
                                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        }

        @Test
        void givenExistingTicket_whenUpdateTicket_thenReturnsUpdatedTicket() throws Exception {
                TicketResponseDTO updatedTicket = createResponseDTO(1L, "C1", BigDecimal.valueOf(250.0),
                                TicketStatus.AVAILABLE, null);

                when(ticketService.updateTicket(eq(1L), any())).thenReturn(updatedTicket);

                mockMvc.perform(put("/api/v1/tickets/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"seatNumber\":\"C1\",\"price\":250.0,\"status\":\"AVAILABLE\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
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
                TicketResponseDTO reservedTicket = createResponseDTO(1L, "A1", BigDecimal.valueOf(100.0),
                                TicketStatus.RESERVED,
                                LocalDateTime.now());

                when(ticketService.reserveTicket(1L)).thenReturn(reservedTicket);

                mockMvc.perform(post("/api/v1/tickets/{id}/reserve", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("RESERVED"))
                                .andExpect(jsonPath("$.reservedAt").isNotEmpty());
        }

        @Test
        void givenReservedTicket_whenPurchase_thenReturnsSoldTicket() throws Exception {
                TicketResponseDTO soldTicket = createResponseDTO(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD,
                                null);

                when(ticketService.purchaseTicket(1L)).thenReturn(soldTicket);

                mockMvc.perform(post("/api/v1/tickets/{id}/purchase", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.status").value("SOLD"))
                                .andExpect(jsonPath("$.reservedAt").isEmpty());
        }

        @Test
        void givenSoldTicket_whenPurchase_thenThrowsException() throws Exception {
                when(ticketService.purchaseTicket(1L))
                                .thenThrow(new RuntimeException("Satın almak için önce rezervasyon yapmalısınız!"));

                mockMvc.perform(post("/api/v1/tickets/{id}/purchase", 1L))
                                .andExpect(status().isInternalServerError());
        }
}