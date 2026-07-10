package com.omniticket.reservation_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.OutboxEvent;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.repository.OutboxRepository;
import com.omniticket.reservation_service.common.lock.DistributedLockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final DistributedLockTemplate lockTemplate;

    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        lockTemplate.executeWithLock("outbox-poller-lock", () -> {

            List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

            if (pendingEvents.isEmpty()) {
                log.info("There is nothing to process in outbox table");
                return null;
            }

            log.info("Number of outbox messages to process: {}", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                try {
                    TicketPurchaseMessage message = objectMapper.readValue(event.getPayload(),
                            TicketPurchaseMessage.class);

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE_NAME,
                            RabbitMQConfig.ROUTING_KEY,
                            message);

                    event.setStatus("SENT");
                    event.setProcessedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
                    outboxRepository.save(event);

                } catch (Exception e) {
                    log.error("Outbox message could not be sent to RabbitMQ! Event ID: {} - Error: {}", event.getId(),
                            e.getMessage());
                }
            }

            return null;
        });
    }
}