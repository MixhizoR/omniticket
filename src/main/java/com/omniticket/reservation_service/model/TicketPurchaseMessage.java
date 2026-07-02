package com.omniticket.reservation_service.model;

import java.math.BigDecimal;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketPurchaseMessage implements Serializable {
    private Long ticketId;
    private String seatNumber;
    private String userEmail;
    private BigDecimal price;
}
