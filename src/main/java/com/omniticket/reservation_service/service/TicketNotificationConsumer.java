package com.omniticket.reservation_service.service;

import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import jakarta.mail.MessagingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketNotificationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleTicketPurchaseMessage(TicketPurchaseMessage message) {
        log.info("Mesaj alındı: {}", message);
        processNotification(message);
    }

    private void processNotification(TicketPurchaseMessage message) {
        log.info("PDF Fatura simülasyonu başlatıldı...");

        try {
            emailService.sendTicketEmail(
                    message.getUserEmail(),
                    message.getSeatNumber(),
                    message.getPrice());
            log.info("✅ Mail başarıyla gönderildi: {}", message.getUserEmail());
        } catch (MessagingException e) {
            log.error("❌ Mail gönderilemedi: {}", e.getMessage());
            throw new RuntimeException("Mail gönderilemedi", e);
        }
    }
}