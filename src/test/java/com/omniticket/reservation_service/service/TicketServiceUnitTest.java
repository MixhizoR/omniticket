package com.omniticket.reservation_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.common.lock.DistributedLockTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Mock
    private StringRedisTemplate stringRedisTemplate;

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
        ticketService = new TicketService(ticketRepository, outboxRepository, objectMapper, distributedLockTemplate,
                stringRedisTemplate);
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
        Page<Ticket> ticketPage = new PageImpl<>(tickets);
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(ticketPage);

        Page<TicketResponseDTO> result = ticketService.getAllTickets(PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals("A1", result.getContent().get(0).seatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.getContent().get(0).price());
        assertEquals("A2", result.getContent().get(1).seatNumber());
        assertEquals(BigDecimal.valueOf(150.0), result.getContent().get(1).price());
        verify(ticketRepository).findAll(any(Pageable.class));
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
    void givenReservedTicket_whenPurchase_thenSavesOutboxAndReturnsSoldTicket() {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);
        soldTicket.setReservedAt(null);

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:test-key-123")).thenReturn(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(soldTicket);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponseDTO result = ticketService.purchaseTicket(1L, "test-key-123");

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

        verify(stringRedisTemplate.opsForValue()).set(eq("idempotency:purchase:test-key-123"), eq("1"), eq(24L),
                eq(TimeUnit.HOURS));
    }

    @Test
    void givenNonReservedTicket_whenPurchase_thenThrowsIllegalStateException() {
        Ticket availableTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:test-key-123")).thenReturn(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(availableTicket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.purchaseTicket(1L, "test-key-123"));
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenSoldTicket_whenPurchase_thenThrowsIllegalStateException() {
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:test-key-123")).thenReturn(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(soldTicket));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ticketService.purchaseTicket(1L, "test-key-123"));
        assertTrue(exception.getMessage().contains("not reserved"));

        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenLockNotAcquired_whenPurchase_thenThrowsTicketLockAcquisitionException() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:test-key-123")).thenReturn(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any()))
                .thenThrow(new TicketLockAcquisitionException("System is busy, please try again!"));

        TicketLockAcquisitionException exception = assertThrows(TicketLockAcquisitionException.class,
                () -> ticketService.purchaseTicket(1L, "test-key-123"));
        assertTrue(exception.getMessage().contains("busy"));

        verify(ticketRepository, never()).findById(anyLong());
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenInterruptedLock_whenPurchase_thenThrowsTicketSystemException() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:test-key-123")).thenReturn(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any()))
                .thenThrow(
                        new TicketSystemException("A system error occurred.", new InterruptedException("interrupted")));

        TicketSystemException exception = assertThrows(TicketSystemException.class,
                () -> ticketService.purchaseTicket(1L, "test-key-123"));
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

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:test-key-123")).thenReturn(null);

        ticketService = new TicketService(ticketRepository, outboxRepository, spyMapper, distributedLockTemplate,
                stringRedisTemplate);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ticketService.purchaseTicket(1L, "test-key-123"));
        assertTrue(exception.getMessage().contains("JSON serialization error"));

        verify(ticketRepository).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenBlankIdempotencyKey_whenPurchase_thenThrowsTicketSystemException() {
        TicketSystemException exception = assertThrows(TicketSystemException.class,
                () -> ticketService.purchaseTicket(1L, "  "));
        assertTrue(exception.getMessage().contains("Idempotency-Key is required"));
        verify(stringRedisTemplate, never()).opsForValue();
        verify(distributedLockTemplate, never()).executeWithLock(anyString(), any());
    }

    @Test
    void givenNullIdempotencyKey_whenPurchase_thenThrowsTicketSystemException() {
        TicketSystemException exception = assertThrows(TicketSystemException.class,
                () -> ticketService.purchaseTicket(1L, null));
        assertTrue(exception.getMessage().contains("Idempotency-Key is required"));
        verify(stringRedisTemplate, never()).opsForValue();
        verify(distributedLockTemplate, never()).executeWithLock(anyString(), any());
    }

    @Test
    void givenDuplicateIdempotencyKey_whenPurchase_thenReturnsExistingTicket() {
        Ticket alreadySoldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);
        alreadySoldTicket.setReservedAt(null);

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:existing-key-456")).thenReturn("1");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(alreadySoldTicket));

        TicketResponseDTO result = ticketService.purchaseTicket(1L, "existing-key-456");

        assertNotNull(result);
        assertEquals(TicketStatus.SOLD, result.status());

        verify(stringRedisTemplate.opsForValue()).get("idempotency:purchase:existing-key-456");
        verify(ticketRepository).findById(1L);
        verify(stringRedisTemplate.opsForValue(), never()).set(anyString(), anyString(), anyLong(), any());
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(distributedLockTemplate, never()).executeWithLock(anyString(), any());
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenValidIdempotencyKey_whenPurchase_thenKeyIsStoredInRedis() {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);
        soldTicket.setReservedAt(null);

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("idempotency:purchase:new-key-789")).thenReturn(null);

        when(distributedLockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<TicketResponseDTO> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(soldTicket);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.purchaseTicket(1L, "new-key-789");

        verify(stringRedisTemplate.opsForValue()).set(eq("idempotency:purchase:new-key-789"), eq("1"), eq(24L),
                eq(TimeUnit.HOURS));
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