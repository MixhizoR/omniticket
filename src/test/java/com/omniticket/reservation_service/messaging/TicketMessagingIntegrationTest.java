package com.omniticket.reservation_service.messaging;

import com.omniticket.reservation_service.AbstractBaseIntegrationTest;
import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class TicketMessagingIntegrationTest extends AbstractBaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestMessagesHolder testMessagesHolder;

    @BeforeEach
    void setUp() {
        testMessagesHolder.clear();
    }

    @Test
    void givenTicketPurchase_whenPurchase_thenMessageSentToRabbitMQ() {
        // Send 2 messages to ensure at least one reaches TestMessagesHolder
        // (RabbitMQ round-robins between TicketNotificationConsumer and
        // TestMessagesHolder)
        TicketPurchaseMessage msg = new TicketPurchaseMessage(1L, "A1", "test@example.com", BigDecimal.valueOf(100.0));
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, msg);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, msg);

        await().atMost(10, TimeUnit.SECONDS).until(() -> !testMessagesHolder.getMessages().isEmpty());
        assertFalse(testMessagesHolder.getMessages().isEmpty());
    }

    @Test
    void givenTicketPurchaseMessage_whenConsumed_thenMessageReceived() {
        // Send 2 messages to ensure at least one reaches TestMessagesHolder
        TicketPurchaseMessage msg = new TicketPurchaseMessage(2L, "B2", "user@example.com", BigDecimal.valueOf(200.0));
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, msg);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, msg);

        await().atMost(10, TimeUnit.SECONDS).until(
                () -> testMessagesHolder.getMessages().stream().anyMatch(m -> m.getTicketId().equals(2L)));

        TicketPurchaseMessage received = testMessagesHolder.getMessages().stream()
                .filter(m -> m.getTicketId().equals(2L))
                .findFirst()
                .orElse(null);

        assertNotNull(received);
        assertEquals("B2", received.getSeatNumber());
        assertEquals("user@example.com", received.getUserEmail());
        assertEquals(BigDecimal.valueOf(200.0), received.getPrice());
    }
}