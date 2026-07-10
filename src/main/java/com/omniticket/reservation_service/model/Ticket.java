package com.omniticket.reservation_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(nullable = true)
    private LocalDateTime reservedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public void reserve() {
        if (this.status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException("Ticket is not available");
        }
        this.status = TicketStatus.RESERVED;
        this.reservedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    public void purchase() {
        if (this.status != TicketStatus.RESERVED) {
            throw new IllegalStateException("Ticket is not reserved");
        }
        if (this.status == TicketStatus.SOLD) {
            throw new IllegalStateException("Ticket is already sold");
        }

        this.status = TicketStatus.SOLD;
        this.reservedAt = null;
    }

    public void release() {
        if (this.status != TicketStatus.RESERVED) {
            throw new IllegalStateException("Ticket is not reserved");
        }
        if (this.status == TicketStatus.AVAILABLE) {
            throw new IllegalStateException("Ticket is already available");
        }
        this.status = TicketStatus.AVAILABLE;
        this.reservedAt = null;
    }
}
