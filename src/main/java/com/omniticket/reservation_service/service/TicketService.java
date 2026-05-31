package com.omniticket.reservation_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import com.omniticket.reservation_service.config.RabbitMQConfig;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.exception.TicketAlreadyReservedException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final RabbitTemplate rabbitTemplate;

    public Ticket createTicket(Ticket ticket) {
        log.info("Yeni bilet oluşturuluyor: {}", ticket.getSeatNumber());
        return ticketRepository.save(ticket);
    }

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAllByOrderByIdAsc();
    }

    public Ticket updateTicket(Long id, Ticket ticketDetails) {
        Ticket existingTicket = getTicket(id);
        existingTicket.setSeatNumber(ticketDetails.getSeatNumber());
        existingTicket.setPrice(ticketDetails.getPrice());
        existingTicket.setStatus(ticketDetails.getStatus());
        log.info("Bilet güncellendi: {}", id);
        return ticketRepository.save(existingTicket);
    }

    public void deleteTicket(Long id) {
        Ticket ticket = getTicket(id);
        ticketRepository.delete(ticket);
        log.warn("Bilet silindi: {}", id);
    }

    public Ticket reserveTicket(Long id) {
        RLock lock = redissonClient.getLock("ticket-lock:" + id);

        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Şu an çok yoğun, lütfen tekrar deneyin!");
            }

            try {
                log.info("Kilit alındı, işlem başlıyor... 🔐");

                return transactionTemplate.execute(status -> {
                    Ticket ticket = ticketRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Bilet bulunamadı!"));

                    if (ticket.getStatus() != TicketStatus.AVAILABLE) {
                        throw new TicketAlreadyReservedException("This ticket is already sold/reserved!");
                    }

                    ticket.setStatus(TicketStatus.RESERVED);
                    ticket.setReservedAt(LocalDateTime.now());

                    return ticketRepository.save(ticket);
                });

            } finally {
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("İşlem bitti, kilit açıldı.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sistemsel bir hata oluştu.");
        }
    }

    public Ticket purchaseTicket(Long id) {
        RLock lock = redissonClient.getLock("ticket-lock:" + id);

        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Ticket is currently being processed, please try again.");
            }

            try {
                log.info("Kilit alındı, işlem başlıyor... 🔐");

                Ticket soldTicket = transactionTemplate.execute(status -> {
                    Ticket ticket = ticketRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Bilet bulunamadı! ID: " + id));

                    if (ticket.getStatus() != TicketStatus.RESERVED) {
                        throw new TicketAlreadyReservedException("This ticket is already sold/reserved!");
                    }

                    ticket.setStatus(TicketStatus.SOLD);
                    ticket.setReservedAt(null);

                    log.info("Bilet başarıyla satıldı: {}", id);
                    return ticketRepository.save(ticket);
                });

                // RabbitMQ mesajı transaction başarıyla commit edildikten SONRA gönderilir
                TicketPurchaseMessage message = new TicketPurchaseMessage(
                        soldTicket.getId(),
                        soldTicket.getSeatNumber(),
                        "no-reply@omniticket.com",
                        soldTicket.getPrice());

                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, message);
                log.info("RabbitMQ'ya mesaj fırlatıldı: {}", message);

                return soldTicket;

            } finally {
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("İşlem bitti, kilit açıldı.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sistemsel bir hata oluştu.");
        }
    }
}