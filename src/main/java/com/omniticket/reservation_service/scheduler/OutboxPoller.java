package com.omniticket.reservation_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.OutboxEvent;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.repository.OutboxRepository;
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

    // Her 5 saniyede bir çalışacak
    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        // 1. PENDING olan son 50 mesajı çek
        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        if (pendingEvents.isEmpty()) {
            log.info("There is nothing to process in outbox table");
            return;
        }

        log.info("Number of outbox messages to process: {}", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // 2. DB'de tuttuğumuz JSON'ı tekrar Objeye çevir
                TicketPurchaseMessage message = objectMapper.readValue(event.getPayload(), TicketPurchaseMessage.class);

                // 3. RabbitMQ'ya gönder
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.ROUTING_KEY,
                        message);

                // 4. Başarıyla gittiyse statüyü SENT yap
                event.setStatus("SENT");
                event.setProcessedAt(LocalDateTime.now(java.time.ZoneId.of("UTC+3")));
                outboxRepository.save(event);

            } catch (Exception e) {
                // 5. Hata olursa statüyü değiştirme! Bir sonraki 5 saniyede tekrar dene.
                log.error("Outbox message could not be sent to RabbitMQ! Event ID: {} - Error: {}", event.getId(),
                        e.getMessage());
                // Döngüde bir sonraki kayda geç, bu kaydı atla.
            }
        }
    }
}