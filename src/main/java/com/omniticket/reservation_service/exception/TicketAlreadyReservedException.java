package com.omniticket.reservation_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class TicketAlreadyReservedException extends RuntimeException {
    public TicketAlreadyReservedException(String message) {
        super(message);
    }
}
