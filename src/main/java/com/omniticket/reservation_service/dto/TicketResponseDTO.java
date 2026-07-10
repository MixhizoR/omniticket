package com.omniticket.reservation_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.omniticket.reservation_service.model.TicketStatus;

public record TicketResponseDTO(Long id, String seatNumber, BigDecimal price, TicketStatus status,
                LocalDateTime reservedAt) {
}
