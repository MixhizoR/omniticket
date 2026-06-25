package com.omniticket.reservation_service.dto;

import com.omniticket.reservation_service.model.TicketStatus;

import lombok.Data;

@Data
public class TicketCreateRequestDTO {

    private String seatNumber;
    private Double price;
    private TicketStatus status;

}
