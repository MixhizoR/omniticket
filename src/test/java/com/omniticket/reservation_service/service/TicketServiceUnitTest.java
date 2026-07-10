package com.omniticket.reservation_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.common.lock.DistributedLockTemplate;
import com.omniticket.reservation_service.dto.TicketRequestDTO;
import com.omniticket.reservation_service.dto.TicketResponseDTO;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.exception.TicketLockAcquisitionException;
import com.omniticket.reservation_service.exception.TicketSystemException;
import com.omniticket.reservation_service.model.OutboxEvent;
import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.OutboxRepository;
import com.omniticket.reservation_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceUnitTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private DistributedLockTemplate distributedLockTemplate;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private TicketService ticketService;
    private ObjectMapper objectMapper;

    private Ticket createTicket(Long id, String seatNumber, BigDecimal price, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
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
        objectMapper = new ObjectMapper();
        ticketService = new TicketService(ticketRepository, outboxRepository, objectMapper, distributedLockTemplate);
    }

    @Test
    void givenValidTicket_whenCreateTicket_thenReturnsSavedTicket() {
        TicketRequestDTO requestDTO = new TicketRequestDTO("A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);
        Ticket savedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        TicketResponseDTO result = ticketService.createTicket(requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("A1", result.seatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.price());
        assertEquals(TicketStatus.AVAILABLE, result.status());
        verify(ticketRepository).save(any(Ticket.class));
        verify(ticketRepository).save(ticketCaptor.capture());
        Ticket captured = ticketCaptor.getValue();
        assertEquals("A1", captured.getSeatNumber());
        assertEquals(BigDecimal.valueOf(100.0), captured.getPrice());
        assertEquals(TicketStatus.AVAILABLE, captured.getStatus());
    }

    @Test
    void givenExistingTicketId_whenGetTicket_thenReturnsTicket() {
        Ticket ticket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponseDTO result = ticketService.getTicket(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("A1", result.seatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.price());
        assertEquals(TicketStatus.AVAILABLE, result.status());
        verify(ticketRepository).findById(1L);
    }

    @Test
    void givenNonExistingTicketId_whenGetTicket_thenThrowsResourceNotFoundException() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.getTicket(999L));
        verify(ticketRepository).findById(999L);
    }

    @Test
    void givenTicketList_whenGetAllTickets_thenReturnsSortedTicketList() {
        List<Ticket> tickets = List.of(
                createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE),
                createTicket(2L, "A2", BigDecimal.valueOf(150.0), TicketStatus.AVAILABLE));
        when(ticketRepository.findAllByOrderByIdAsc()).thenReturn(tickets);

        List<TicketResponseDTO> result = ticketService.getAllTickets();

        assertEquals(2, result.size());
        assertEquals("A1", result.get(0).seatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.get(0).price());
        assertEquals("A2", result.get(1).seatNumber());
        assertEquals(BigDecimal.valueOf(150.0), result.get(1).price());
        verify(ticketRepository).findAllByOrderByIdAsc();
    }

    @Test
    void givenExistingTicket_whenUpdateTicket_thenUpdatesAndReturnsTicket() {
        Ticket existingTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);
        TicketRequestDTO updateDTO = new TicketRequestDTO("B1", BigDecimal.valueOf(200.0), TicketStatus.RESERVED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        TicketResponseDTO result = ticketService.updateTicket(1L, updateDTO);

        assertNotNull(result);
        assertEquals("B1", result.seatNumber());
        assertEquals(BigDecimal.valueOf(200.0), result.price());
        assertEquals(TicketStatus.RESERVED, result.status());
        assertNotNull(result.reservedAt());
        verify(ticketRepository).findById(1L);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void givenExistingTicket_whenDeleteTicket_thenRemovesFromDatabase() {
        ticketService.deleteTicket(1L);

        verify(ticketRepository).deleteById(1L);
    }

    @Test
    void givenTicketNotFound_whenDeleteTicket_thenDoesNotThrow() {
        doNothing().when(ticketRepository).deleteById(1L);

        assertDoesNotThrow(() -> ticketService.deleteTicket(1L));
        verify(ticketRepository).deleteById(1L);
    }

    @Test
    void givenAvailableTicket_whenReserve_thenReturnsReservedTicket() {
        Ticket ticket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponseDTO result = ticketService.reserveTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.RESERVED, result.status());
        assertNotNull(result.reservedAt());
        verify(distributedLockTemplate).executeWithLock(eq("ticket-lock:" + 1L), any());
    }

    @Test
    void givenTicketAlreadyReserved_whenReserve_thenThrowsIllegalStateException() {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));

        assertThrows(IllegalStateException.class, () -> ticketService.reserveTicket(1L));

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void givenRedisConnection_whenLockFails_thenThrowsTicketLockAcquisitionException() {
        when(distributedLockTemplate.executeWithLock(anyString(), any()))
                .thenThrow(new TicketLockAcquisitionException("System is busy, please try again!"));

        TicketLockAcquisitionException exception = assertThrows(TicketLockAcquisitionException.class,
                () -> ticketService.reserveTicket(1L));
        assertTrue(exception.getMessage().contains("busy"));

        verify(ticketRepository, never()).findById(anyLong());
    }

    @Test
    void givenInterruptedLock_whenReserve_thenThrowsTicketSystemException() {
        when(distributedLockTemplate.executeWithLock(anyString(), any()))
                .thenThrow(
                        new TicketSystemException("A system error occurred.", new InterruptedException("interrupted")));

        TicketSystemException exception = assertThrows(TicketSystemException.class,
                () -> ticketService.reserveTicket(1L));
        assertTrue(exception.getMessage().contains("system error"));

        verify(ticketRepository, never()).findById(anyLong());
    }

    @Test
    void givenReservedTicket_whenPurchase_thenSavesOutboxAndReturnsSoldTicket() throws Exception {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);
        soldTicket.setReservedAt(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(soldTicket);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponseDTO result = ticketService.purchaseTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.SOLD, result.status());
        assertNull(result.reservedAt());

        verify(distributedLockTemplate).executeWithLock(eq("ticket-lock:" + 1L), any());
        verify(ticketRepository).findById(1L);
        verify(ticketRepository).save(any(Ticket.class));

        verify(outboxRepository).save(outboxEventCaptor.capture());
        OutboxEvent capturedEvent = outboxEventCaptor.getValue();
        assertEquals("1", capturedEvent.getAggregateId());
        assertEquals("TICKET_SOLD", capturedEvent.getEventType());
        assertNotNull(capturedEvent.getPayload());

        String payload = capturedEvent.getPayload();
        assertTrue(payload.contains("A1"));
        assertTrue(payload.contains("100.0"));

        verify(ticketRepository, never()).deleteById(anyLong());
    }

    @Test
    void givenNonReservedTicket_whenPurchase_thenThrowsIllegalStateException() {
        Ticket availableTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(availableTicket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.purchaseTicket(1L));
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenSoldTicket_whenPurchase_thenThrowsIllegalStateException() {
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(soldTicket));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("not reserved"));

        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenLockNotAcquired_whenPurchase_thenThrowsTicketLockAcquisitionException() {
        when(distributedLockTemplate.executeWithLock(anyString(), any()))
                .thenThrow(new TicketLockAcquisitionException("System is busy, please try again!"));

        TicketLockAcquisitionException exception = assertThrows(TicketLockAcquisitionException.class,
                () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("busy"));

        verify(ticketRepository, never()).findById(anyLong());
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenInterruptedLock_whenPurchase_thenThrowsTicketSystemException() {
        when(distributedLockTemplate.executeWithLock(anyString(), any()))
                .thenThrow(
                        new TicketSystemException("A system error occurred.", new InterruptedException("interrupted")));

        TicketSystemException exception = assertThrows(TicketSystemException.class,
                () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("system error"));

        verify(ticketRepository, never()).findById(anyLong());
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenJsonProcessingError_whenPurchase_thenThrowsRuntimeException() throws Exception {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);

        ObjectMapper spyMapper = spy(new ObjectMapper());
        doThrow(new JsonProcessingException("mock json error") {
        }).when(spyMapper)
                .writeValueAsString(any(TicketPurchaseMessage.class));

        ticketService = new TicketService(ticketRepository, outboxRepository, spyMapper, distributedLockTemplate);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("JSON serialization error"));

        verify(ticketRepository).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenExistingTicketId_whenGetTicketWithReservedStatus_thenReturnsWithReservedAt() {
        LocalDateTime reservedAt = LocalDateTime.of(2026, Month.JUNE, 29, 15, 0);
        Ticket ticket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        ticket.setReservedAt(reservedAt);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponseDTO result = ticketService.getTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.RESERVED, result.status());
        assertEquals(reservedAt, result.reservedAt());
    }
}