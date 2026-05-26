package com.omniticket.reservation_service.service;

import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.model.TicketStatus;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    private RedissonClient redissonClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RLock rLock;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @Captor
    private ArgumentCaptor<TicketPurchaseMessage> messageCaptor;

    private TicketService ticketService;

    private Ticket createTicket(Long id, String seatNumber, Double price, TicketStatus status) {
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
        ticketService = new TicketService(ticketRepository, redissonClient, transactionTemplate, rabbitTemplate);
    }

    @Test
    void givenValidTicket_whenCreateTicket_thenReturnsSavedTicket() {
        Ticket ticket = createTicket(null, "A1", 100.0, TicketStatus.AVAILABLE);
        Ticket savedTicket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);

        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        Ticket result = ticketService.createTicket(ticket);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("A1", result.getSeatNumber());
        assertEquals(TicketStatus.AVAILABLE, result.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void givenExistingTicketId_whenGetTicket_thenReturnsTicket() {
        Ticket ticket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.getTicket(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
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
                createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE),
                createTicket(2L, "A2", 150.0, TicketStatus.AVAILABLE));
        when(ticketRepository.findAllByOrderByIdAsc()).thenReturn(tickets);

        List<Ticket> result = ticketService.getAllTickets();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(ticketRepository).findAllByOrderByIdAsc();
    }

    @Test
    void givenExistingTicket_whenUpdateTicket_thenUpdatesAndReturnsTicket() {
        Ticket existingTicket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);
        Ticket updateDetails = createTicket(1L, "B1", 200.0, TicketStatus.RESERVED);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = ticketService.updateTicket(1L, updateDetails);

        assertNotNull(result);
        assertEquals("B1", result.getSeatNumber());
        assertEquals(200.0, result.getPrice());
        assertEquals(TicketStatus.RESERVED, result.getStatus());
        verify(ticketRepository).findById(1L);
        verify(ticketRepository).save(existingTicket);
    }

    @Test
    void givenExistingTicket_whenDeleteTicket_thenRemovesFromDatabase() {
        Ticket ticket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicket(1L);

        verify(ticketRepository).findById(1L);
        verify(ticketRepository).delete(ticket);
    }

    @Test
    void givenRedisConnection_whenGetLock_thenReturnsRLock() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);

        Ticket ticket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<Ticket> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(rLock.isLocked()).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        Ticket result = ticketService.reserveTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.RESERVED, result.getStatus());
        verify(redissonClient).getLock("ticket-lock:" + 1L);
        verify(rLock).tryLock(5, 10, TimeUnit.SECONDS);
        verify(rLock).unlock();
    }

    @Test
    void givenTicketAlreadyReserved_whenReserve_thenThrowsRuntimeException() throws InterruptedException {
        Ticket reservedTicket = createTicket(1L, "A1", 100.0, TicketStatus.RESERVED);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<Ticket> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(rLock.isLocked()).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        assertThrows(RuntimeException.class, () -> ticketService.reserveTicket(1L));

        verify(rLock).unlock();
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void givenReservedTicket_whenPurchase_thenPublishesMessageAndReturnsTicket() {
        Ticket reservedTicket = createTicket(1L, "A1", 100.0, TicketStatus.RESERVED);
        Ticket soldTicket = createTicket(1L, "A1", 100.0, TicketStatus.SOLD);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(reservedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(soldTicket);

        Ticket result = ticketService.purchaseTicket(1L);

        assertNotNull(result);
        assertEquals(TicketStatus.SOLD, result.getStatus());
        assertNull(result.getReservedAt());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                any(Object.class));
    }

    @Test
    void givenNonReservedTicket_whenPurchase_thenThrowsRuntimeException() {
        Ticket availableTicket = createTicket(1L, "A1", 100.0, TicketStatus.AVAILABLE);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(availableTicket));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ticketService.purchaseTicket(1L));
        assertTrue(exception.getMessage().contains("rezervasyon"));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}