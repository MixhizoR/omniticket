package com.omniticket.reservation_service.dto;

import java.math.BigDecimal;

import com.omniticket.reservation_service.model.TicketStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TicketRequestDTO(
        @NotBlank(message = "Seat number is required") String seatNumber,

        @Positive(message = "Price must be positive") BigDecimal price,

        @NotNull(message = "Status is required") TicketStatus status) {
}
