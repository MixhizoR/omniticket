package com.omniticket.reservation_service.messaging;

import com.omniticket.reservation_service.BaseIntegrationTest;
import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketMessagingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestMessagesHolder testMessagesHolder;

    @Test
    void givenTicketPurchase_whenPurchase_thenMessageSentToRabbitMQ() {
        TicketPurchaseMessage message = new TicketPurchaseMessage(1L, "A1", "test@example.com", 100.0);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, message);

        await().atMost(10, TimeUnit.SECONDS).until(() -> !testMessagesHolder.getMessages().isEmpty());
        assertFalse(testMessagesHolder.getMessages().isEmpty());
    }

    @Test
    void givenTicketPurchaseMessage_whenConsumed_thenMessageReceived() {
        TicketPurchaseMessage message = new TicketPurchaseMessage(2L, "B2", "user@example.com", 200.0);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, message);

        await().atMost(10, TimeUnit.SECONDS).until(
                () -> testMessagesHolder.getMessages().stream().anyMatch(m -> m.getTicketId().equals(2L)));

        TicketPurchaseMessage received = testMessagesHolder.getMessages().stream()
                .filter(m -> m.getTicketId().equals(2L))
                .findFirst()
                .orElse(null);

        assertNotNull(received);
        assertEquals("B2", received.getSeatNumber());
        assertEquals("user@example.com", received.getUserEmail());
        assertEquals(200.0, received.getPrice(), 0.001);
    }
}