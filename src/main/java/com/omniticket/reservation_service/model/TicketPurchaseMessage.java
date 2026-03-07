package com.omniticket.reservation_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketPurchaseMessage implements Serializable {
    private Long ticketId;
    private String seatNumber;
    private String userEmail;
    private double price;
}
