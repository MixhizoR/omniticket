package com.omniticket.reservation_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.exception.TicketAlreadyReservedException;
import com.omniticket.reservation_service.exception.TicketLockAcquisitionException;
import com.omniticket.reservation_service.exception.TicketSystemException;
import com.omniticket.reservation_service.model.OutboxEvent;
import com.omniticket.reservation_service.repository.OutboxRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.common.lock.DistributedLockTemplate;
import com.omniticket.reservation_service.dto.TicketCreateRequestDTO;
import com.omniticket.reservation_service.dto.TicketResponseDTO;
import com.omniticket.reservation_service.dto.TicketUpdateRequestDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DistributedLockTemplate distributedLockTemplate;

    @Transactional
    public TicketResponseDTO createTicket(TicketCreateRequestDTO ticketCreateRequestDTO) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(ticketCreateRequestDTO.getSeatNumber());
        ticket.setPrice(ticketCreateRequestDTO.getPrice());
        ticket.setStatus(ticketCreateRequestDTO.getStatus());
        log.info("Yeni bilet oluşturuluyor: {}", ticket.getSeatNumber());
        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToResponseDTO(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicket(Long id) {
        Ticket ticket = findTicketById(id);
        return mapToResponseDTO(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getAllTickets() {
        List<Ticket> tickets = ticketRepository.findAllByOrderByIdAsc();
        return tickets.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional
    public TicketResponseDTO updateTicket(Long id, TicketUpdateRequestDTO ticketDetails) {
        Ticket existingTicket = findTicketById(id);
        existingTicket.setSeatNumber(ticketDetails.getSeatNumber());
        existingTicket.setPrice(ticketDetails.getPrice());
        existingTicket.setStatus(ticketDetails.getStatus());

        if (ticketDetails.getStatus() == TicketStatus.RESERVED) {
            existingTicket.setReservedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        } else {
            existingTicket.setReservedAt(null);
        }

        log.info("Bilet güncellendi: {}", id);
        return mapToResponseDTO(existingTicket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
        log.warn("Bilet silindi: {}", id);
    }

    public TicketResponseDTO reserveTicket(Long id) {
        return distributedLockTemplate.executeWithLock("ticket-lock:" + id, () -> {
            Ticket ticket = findTicketById(id);
            if (ticket.getStatus() != TicketStatus.AVAILABLE) {
                throw new TicketAlreadyReservedException("Bu bilet zaten satılmış veya rezerve edilmiş!");
            }
            ticket.setStatus(TicketStatus.RESERVED);
            ticket.setReservedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
            Ticket savedTicket = ticketRepository.save(ticket);
            return mapToResponseDTO(savedTicket);
        });
    }

    public TicketResponseDTO purchaseTicket(Long id) {
        return distributedLockTemplate.executeWithLock("ticket-lock:" + id, () -> {

            Ticket ticket = findTicketById(id);

            if (ticket.getStatus() != TicketStatus.RESERVED) {
                throw new TicketAlreadyReservedException("Bu bilet zaten satılmış veya rezerve edilmemiş!");
            }

            ticket.setStatus(TicketStatus.SOLD);
            ticket.setReservedAt(null);
            Ticket soldTicket = ticketRepository.save(ticket);

            try {
                String payloadJson = objectMapper.writeValueAsString(
                        new TicketPurchaseMessage(
                                soldTicket.getId(),
                                soldTicket.getSeatNumber(),
                                "no-reply@omniticket.com",
                                soldTicket.getPrice()));

                OutboxEvent event = new OutboxEvent();
                event.setAggregateId(String.valueOf(soldTicket.getId()));
                event.setEventType("TICKET_SOLD");
                event.setPayload(payloadJson);
                outboxRepository.save(event);

                return mapToResponseDTO(soldTicket);

            } catch (JsonProcessingException e) {
                throw new TicketSystemException("JSON serileştirme hatası nedeniyle satın alma iptal edildi.", e);
            }
        });
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    private TicketResponseDTO mapToResponseDTO(Ticket ticket) {
        return new TicketResponseDTO(
                ticket.getId(),
                ticket.getSeatNumber(),
                ticket.getPrice(),
                ticket.getStatus(),
                ticket.getReservedAt());
    }
}
