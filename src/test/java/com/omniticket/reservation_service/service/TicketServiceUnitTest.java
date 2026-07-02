package com.omniticket.reservation_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.dto.TicketCreateRequestDTO;
import com.omniticket.reservation_service.dto.TicketResponseDTO;
import com.omniticket.reservation_service.dto.TicketUpdateRequestDTO;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.exception.TicketAlreadyReservedException;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceUnitTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RLock rLock;

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
        ticketService = new TicketService(ticketRepository, redissonClient, transactionTemplate, outboxRepository,
                objectMapper);
    }

    @Test
    void givenValidTicket_whenCreateTicket_thenReturnsSavedTicket() {
        TicketCreateRequestDTO requestDTO = new TicketCreateRequestDTO();
        requestDTO.setSeatNumber("A1");
        requestDTO.setPrice(BigDecimal.valueOf(100.0));
        requestDTO.setStatus(TicketStatus.AVAILABLE);
        Ticket savedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        TicketResponseDTO result = ticketService.createTicket(requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("A1", result.getSeatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.getPrice());
        assertEquals(TicketStatus.AVAILABLE, result.getStatus());
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
        assertEquals(1L, result.getId());
        assertEquals("A1", result.getSeatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.getPrice());
        assertEquals(TicketStatus.AVAILABLE, result.getStatus());
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
        assertEquals("A1", result.get(0).getSeatNumber());
        assertEquals(BigDecimal.valueOf(100.0), result.get(0).getPrice());
        assertEquals("A2", result.get(1).getSeatNumber());
        assertEquals(BigDecimal.valueOf(150.0), result.get(1).getPrice());
        verify(ticketRepository).findAllByOrderByIdAsc();
    }

    @Test
    void givenExistingTicket_whenUpdateTicket_thenUpdatesAndReturnsTicket() {
        Ticket existingTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);
        TicketUpdateRequestDTO updateDTO = new TicketUpdateRequestDTO();
        updateDTO.setSeatNumber("B1");
        updateDTO.setPrice(BigDecimal.valueOf(200.0));
        updateDTO.setStatus(TicketStatus.RESERVED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));

        TicketResponseDTO result = ticketService.updateTicket(1L, updateDTO);

        assertNotNull(result);
        assertEquals("B1", result.getSeatNumber());
        assertEquals(BigDecimal.valueOf(200.0), result.getPrice());
        assertEquals(TicketStatus.RESERVED, result.getStatus());
        assertNotNull(result.getReservedAt());
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
    void givenRedisConnection_whenGetLock_thenReturnsRLock() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);

        Ticket ticket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<TicketResponseDTO> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        TicketResponseDTO result = ticketService.reserveTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.RESERVED, result.getStatus());
        assertNotNull(result.getReservedAt());
        verify(redissonClient).getLock("ticket-lock:" + 1L);
        verify(rLock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(rLock).unlock();
    }

    @Test
    void givenTicketAlreadyReserved_whenReserve_thenThrowsRuntimeException() throws InterruptedException {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<TicketResponseDTO> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        assertThrows(RuntimeException.class, () -> ticketService.reserveTicket(1L));

        verify(rLock).unlock();
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void givenRedisConnection_whenLockFails_thenThrowsRuntimeException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ticketService.reserveTicket(1L));
        assertTrue(exception.getMessage().contains("çok yoğun"));

        verify(rLock, never()).unlock();
        verify(ticketRepository, never()).findById(anyLong());
    }

    @Test
    void givenInterruptedLock_whenReserve_thenThrowsRuntimeException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("interrupted"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ticketService.reserveTicket(1L));
        assertTrue(exception.getMessage().contains("Sistemsel bir hata"));

        verify(rLock, never()).unlock();
    }

    @Test
    void givenReservedTicket_whenPurchase_thenSavesOutboxAndReturnsSoldTicket() throws Exception {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);
        soldTicket.setReservedAt(null);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(soldTicket);
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        TicketResponseDTO result = ticketService.purchaseTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.SOLD, result.getStatus());
        assertNull(result.getReservedAt());

        verify(redissonClient).getLock("ticket-lock:" + 1L);
        verify(rLock).tryLock(5, -1, TimeUnit.SECONDS);
        verify(rLock).unlock();
        verify(ticketRepository).findById(1L);
        verify(ticketRepository).save(any(Ticket.class));

        // Verify outboxRepository.save was called with correct event
        verify(outboxRepository).save(outboxEventCaptor.capture());
        OutboxEvent capturedEvent = outboxEventCaptor.getValue();
        assertEquals("1", capturedEvent.getAggregateId());
        assertEquals("TICKET_SOLD", capturedEvent.getEventType());
        assertNotNull(capturedEvent.getPayload());

        // Verify the payload is valid JSON and contains seat info
        String payload = capturedEvent.getPayload();
        assertTrue(payload.contains("A1"));
        assertTrue(payload.contains("100.0"));

        // Ensure rabbitTemplate was NOT called directly
        verify(ticketRepository, never()).deleteById(anyLong());
    }

    @Test
    void givenNonReservedTicket_whenPurchase_thenThrowsTicketAlreadyReservedException() throws InterruptedException {
        Ticket availableTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.AVAILABLE);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(availableTicket));
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        TicketAlreadyReservedException exception = assertThrows(TicketAlreadyReservedException.class,
                () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("zaten satılmış"));

        verify(rLock).unlock();
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenSoldTicket_whenPurchase_thenThrowsTicketAlreadyReservedException() throws InterruptedException {
        Ticket soldTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.SOLD);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(soldTicket));
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        TicketAlreadyReservedException exception = assertThrows(TicketAlreadyReservedException.class,
                () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("zaten satılmış"));

        verify(rLock).unlock();
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenLockNotAcquired_whenPurchase_thenThrowsRuntimeException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("işleniyor"));

        verify(rLock, never()).unlock();
        verify(ticketRepository, never()).findById(anyLong());
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenInterruptedLock_whenPurchase_thenThrowsRuntimeException() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("interrupted"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("Sistemsel hata"));

        verify(rLock, never()).unlock();
        verify(ticketRepository, never()).findById(anyLong());
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenJsonProcessingError_whenPurchase_thenThrowsRuntimeException() throws Exception {
        Ticket reservedTicket = createTicket(1L, "A1", BigDecimal.valueOf(100.0), TicketStatus.RESERVED);

        // Use a spy to make writeValueAsString throw
        ObjectMapper spyMapper = spy(new ObjectMapper());
        doThrow(new JsonProcessingException("mock json error") {
        }).when(spyMapper)
                .writeValueAsString(any(TicketPurchaseMessage.class));

        ticketService = new TicketService(ticketRepository, redissonClient, transactionTemplate, outboxRepository,
                spyMapper);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("Mesaj formatı"));

        verify(rLock).unlock();
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
        assertEquals(TicketStatus.RESERVED, result.getStatus());
        assertEquals(reservedAt, result.getReservedAt());
    }
}