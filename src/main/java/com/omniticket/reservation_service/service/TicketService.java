package com.omniticket.reservation_service.service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniticket.reservation_service.exception.TicketSystemException;
import com.omniticket.reservation_service.model.OutboxEvent;
import com.omniticket.reservation_service.repository.OutboxRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketPurchaseMessage;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import com.omniticket.reservation_service.exception.ResourceNotFoundException;
import com.omniticket.reservation_service.common.lock.DistributedLockTemplate;
import com.omniticket.reservation_service.dto.TicketRequestDTO;
import com.omniticket.reservation_service.dto.TicketResponseDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final DistributedLockTemplate distributedLockTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public TicketResponseDTO createTicket(TicketRequestDTO ticketCreateRequestDTO) {
        Ticket ticket = new Ticket();
        ticket.setSeatNumber(ticketCreateRequestDTO.seatNumber());
        ticket.setPrice(ticketCreateRequestDTO.price());
        ticket.setStatus(ticketCreateRequestDTO.status());
        log.info("Creating new ticket: {}", ticket.getSeatNumber());
        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToResponseDTO(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicket(Long id) {
        Ticket ticket = findTicketById(id);
        return mapToResponseDTO(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> getAllTickets(Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findAll(pageable);
        log.info("All tickets fetched.");
        return tickets.map(this::mapToResponseDTO);
    }

    @Transactional
    public TicketResponseDTO updateTicket(Long id, TicketRequestDTO ticketDetails) {
        Ticket existingTicket = findTicketById(id);
        existingTicket.setSeatNumber(ticketDetails.seatNumber());
        existingTicket.setPrice(ticketDetails.price());
        existingTicket.setStatus(ticketDetails.status());

        if (ticketDetails.status() == TicketStatus.RESERVED) {
            existingTicket.setReservedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        } else {
            existingTicket.setReservedAt(null);
        }

        log.info("Ticket updated: {}", id);
        return mapToResponseDTO(existingTicket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
        log.warn("Ticket deleted: {}", id);
    }

    public TicketResponseDTO reserveTicket(Long id) {
        return distributedLockTemplate.executeWithLock("ticket-lock:" + id, () -> {
            Ticket ticket = findTicketById(id);
            ticket.reserve();
            return mapToResponseDTO(ticket);
        });
    }

    public TicketResponseDTO purchaseTicket(Long id, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new TicketSystemException("Idempotency-Key is required.");
        }

        String redisKey = "idempotency:purchase:" + idempotencyKey;

        String existingTicketId = stringRedisTemplate.opsForValue().get(redisKey);
        if (existingTicketId != null) {
            log.info("Idempotency key matched! Returning already purchased ticket.");
            Ticket alreadySoldTicket = findTicketById(Long.valueOf(existingTicketId));
            return mapToResponseDTO(alreadySoldTicket);
        }

        TicketResponseDTO response = distributedLockTemplate.executeWithLock("ticket-lock:" + id, () -> {
            Ticket ticket = findTicketById(id);
            ticket.purchase();
            Ticket soldTicket = ticketRepository.save(ticket);
            saveOutboxEvent(soldTicket);
            return mapToResponseDTO(soldTicket);
        });

        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(id), 24, TimeUnit.HOURS);

        return response;
    }

    private void saveOutboxEvent(Ticket ticket) {
        try {
            String payloadJson = objectMapper.writeValueAsString(
                    new TicketPurchaseMessage(
                            ticket.getId(),
                            ticket.getSeatNumber(),
                            "no-reply@omniticket.com", // TODO change this email to JWT
                            ticket.getPrice()));

            OutboxEvent event = new OutboxEvent();
            event.setAggregateId(String.valueOf(ticket.getId()));
            event.setEventType("TICKET_SOLD");
            event.setPayload(payloadJson);
            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new TicketSystemException("Purchase cancelled due to JSON serialization error.", e);
        }
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
