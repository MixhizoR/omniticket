package com.omniticket.reservation_service.messaging;

import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TestMessagesHolder {

    private final CopyOnWriteArrayList<TicketPurchaseMessage> messages = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(TicketPurchaseMessage message) {
        messages.add(message);
    }

    public CopyOnWriteArrayList<TicketPurchaseMessage> getMessages() {
        return messages;
    }

    public void clear() {
        messages.clear();
    }
}