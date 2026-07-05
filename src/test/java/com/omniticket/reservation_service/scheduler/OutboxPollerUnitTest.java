package com.omniticket.reservation_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.common.lock.DistributedLockTemplate;
import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.OutboxEvent;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollerUnitTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DistributedLockTemplate lockTemplate;

    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;

    private OutboxPoller outboxPoller;
    private ObjectMapper objectMapper;

    private OutboxEvent createPendingEvent(String id, String aggregateId, String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.setId(id);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxPoller = new OutboxPoller(outboxRepository, rabbitTemplate, objectMapper, lockTemplate);
    }

    @Test
    void givenNoPendingEvents_whenProcessOutboxEvents_thenDoesNothing() {
        when(lockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<Void> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of());

        outboxPoller.processOutboxEvents();

        verify(outboxRepository).findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenPendingEvents_whenProcessOutboxEvents_thenSendsToRabbitMQAndMarksSent() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new TicketPurchaseMessage(1L, "A1", "test@test.com", BigDecimal.valueOf(100.0)));
        OutboxEvent event = createPendingEvent("event-1", "1", "TICKET_SOLD", payload);

        when(lockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<Void> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxPoller.processOutboxEvents();

        verify(outboxRepository).findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                any(TicketPurchaseMessage.class));

        verify(outboxRepository).save(outboxEventCaptor.capture());
        OutboxEvent savedEvent = outboxEventCaptor.getValue();
        assertEquals("SENT", savedEvent.getStatus());
        assertEquals("event-1", savedEvent.getId());
        assertNotNull(savedEvent.getProcessedAt());
    }

    @Test
    void givenMultiplePendingEvents_whenProcessOutboxEvents_thenProcessesAll() throws Exception {
        String payload1 = objectMapper.writeValueAsString(
                new TicketPurchaseMessage(1L, "A1", "test1@test.com", BigDecimal.valueOf(100.0)));
        String payload2 = objectMapper.writeValueAsString(
                new TicketPurchaseMessage(2L, "B1", "test2@test.com", BigDecimal.valueOf(200.0)));

        OutboxEvent event1 = createPendingEvent("event-1", "1", "TICKET_SOLD", payload1);
        OutboxEvent event2 = createPendingEvent("event-2", "2", "TICKET_SOLD", payload2);

        when(lockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<Void> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event1, event2));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxPoller.processOutboxEvents();

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                any(TicketPurchaseMessage.class));
        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void givenInvalidPayload_whenProcessOutboxEvents_thenSkipsEventAndLogsError() {
        OutboxEvent event = createPendingEvent("event-1", "1", "TICKET_SOLD", "invalid-json-not-parseable");

        when(lockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<Void> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        outboxPoller.processOutboxEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        // Event should NOT be saved (status stays PENDING for retry)
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenRabbitMqFails_whenProcessOutboxEvents_thenDoesNotMarkAsSent() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new TicketPurchaseMessage(1L, "A1", "test@test.com", BigDecimal.valueOf(100.0)));
        OutboxEvent event = createPendingEvent("event-1", "1", "TICKET_SOLD", payload);

        when(lockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<Void> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        doThrow(new RuntimeException("RabbitMQ connection refused"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        outboxPoller.processOutboxEvents();

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        // Event should NOT be saved (status stays PENDING for retry)
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void givenOneFailingAndOneSucceedingEvent_whenProcessOutboxEvents_thenProcessesOnlySuccessful() throws Exception {
        String validPayload = objectMapper.writeValueAsString(
                new TicketPurchaseMessage(2L, "B1", "test2@test.com", BigDecimal.valueOf(200.0)));

        OutboxEvent invalidEvent = createPendingEvent("event-1", "1", "TICKET_SOLD", "invalid-json");
        OutboxEvent validEvent = createPendingEvent("event-2", "2", "TICKET_SOLD", validPayload);

        when(lockTemplate.executeWithLock(anyString(), any())).thenAnswer(invocation -> {
            Supplier<Void> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(invalidEvent, validEvent));
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxPoller.processOutboxEvents();

        // Only the valid event should be sent and saved
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }
}