package com.omniticket.reservation_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class TicketLockAcquisitionException extends RuntimeException {
    public TicketLockAcquisitionException(String message) {
        super(message);
    }
}