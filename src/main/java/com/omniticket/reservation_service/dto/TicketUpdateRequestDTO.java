package com.omniticket.reservation_service.dto;

import java.math.BigDecimal;

import com.omniticket.reservation_service.model.TicketStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketUpdateRequestDTO {
    private String seatNumber;
    private BigDecimal price;
    private TicketStatus status;
}
