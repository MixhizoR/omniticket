package com.omniticket.reservation_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.omniticket.reservation_service.model.TicketStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TicketResponseDTO {
    private Long id;
    private String seatNumber;
    private BigDecimal price;
    private TicketStatus status;
    private LocalDateTime reservedAt;
}
