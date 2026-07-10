package com.omniticket.reservation_service.dto;

import java.math.BigDecimal;

import com.omniticket.reservation_service.model.TicketStatus;

public record TicketRequestDTO(String seatNumber, BigDecimal price, TicketStatus status) {
}
