package com.omniticket.reservation_service.repository;

import com.omniticket.reservation_service.AbstractBaseIntegrationTest;
import com.omniticket.reservation_service.model.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OutboxRepositoryIntegrationTest extends AbstractBaseIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    private OutboxEvent createEvent(String aggregateId, String eventType, String payload, String status) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(status);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void givenPendingEvent_whenSave_thenPersistsWithAutoFields() {
        OutboxEvent event = createEvent("1", "TICKET_SOLD", "{\"ticketId\":1}", "PENDING");

        OutboxEvent savedEvent = outboxRepository.save(event);

        assertNotNull(savedEvent.getId());
        assertEquals("1", savedEvent.getAggregateId());
        assertEquals("TICKET_SOLD", savedEvent.getEventType());
        assertEquals("PENDING", savedEvent.getStatus());
        assertNotNull(savedEvent.getCreatedAt());
        assertNull(savedEvent.getProcessedAt());
    }

    @Test
    void givenSavedEvent_whenFindById_thenReturnsEvent() {
        OutboxEvent event = createEvent("1", "TICKET_SOLD", "{\"ticketId\":1}", "PENDING");
        OutboxEvent savedEvent = outboxRepository.save(event);

        Optional<OutboxEvent> found = outboxRepository.findById(savedEvent.getId());

        assertTrue(found.isPresent());
        assertEquals(savedEvent.getId(), found.get().getId());
        assertEquals("TICKET_SOLD", found.get().getEventType());
    }

    @Test
    void givenMultiplePendingEvents_whenFindTop50ByStatusOrderByCreatedAtAsc_thenReturnsOldestFirst() {
        OutboxEvent event1 = outboxRepository.save(createEvent("1", "TICKET_SOLD", "{}", "PENDING"));
        OutboxEvent event2 = outboxRepository.save(createEvent("2", "TICKET_SOLD", "{}", "PENDING"));
        // This one gets status "PENDING" due to @PrePersist, so save then update to "SENT"
        OutboxEvent event3 = outboxRepository.save(createEvent("3", "TICKET_SOLD", "{}", "SENT"));
        event3.setStatus("SENT");
        outboxRepository.save(event3);

        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        assertEquals(2, pendingEvents.size());
        assertEquals(event1.getId(), pendingEvents.get(0).getId());
        assertEquals(event2.getId(), pendingEvents.get(1).getId());
    }

    @Test
    void givenNoPendingEvents_whenFindByStatus_thenReturnsEmptyList() {
        OutboxEvent event1 = outboxRepository.save(createEvent("1", "TICKET_SOLD", "{}", "PENDING"));
        event1.setStatus("SENT");
        outboxRepository.save(event1);
        OutboxEvent event2 = outboxRepository.save(createEvent("2", "TICKET_SOLD", "{}", "PENDING"));
        event2.setStatus("SENT");
        outboxRepository.save(event2);

        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        assertTrue(pendingEvents.isEmpty());
    }

    @Test
    void givenSentEvent_whenUpdateStatus_thenCanModify() {
        OutboxEvent event = createEvent("1", "TICKET_SOLD", "{\"ticketId\":1}", "PENDING");
        OutboxEvent savedEvent = outboxRepository.save(event);

        savedEvent.setStatus("SENT");
        savedEvent.setProcessedAt(LocalDateTime.now());
        outboxRepository.save(savedEvent);

        OutboxEvent updated = outboxRepository.findById(savedEvent.getId()).orElseThrow();
        assertEquals("SENT", updated.getStatus());
        assertNotNull(updated.getProcessedAt());
        assertNotNull(updated.getCreatedAt());
    }

    @Test
    void givenMultipleEvents_whenDeleteAll_thenClearsTable() {
        outboxRepository.save(createEvent("1", "TICKET_SOLD", "{}", "PENDING"));
        outboxRepository.save(createEvent("2", "TICKET_SOLD", "{}", "SENT"));

        outboxRepository.deleteAll();

        List<OutboxEvent> all = outboxRepository.findAll();
        assertTrue(all.isEmpty());
    }

    @Test
    void givenEventWithLongPayload_whenSave_thenPersistsCorrectly() {
        String longPayload = "{\"ticketId\":1,\"seatNumber\":\"A1\",\"email\":\"test@test.com\",\"price\":100.0,\"metadata\":\"test\"}";
        OutboxEvent event = createEvent("1", "TICKET_SOLD", longPayload, "PENDING");

        OutboxEvent savedEvent = outboxRepository.save(event);

        assertNotNull(savedEvent.getId());
        assertEquals(longPayload, savedEvent.getPayload());
    }
}