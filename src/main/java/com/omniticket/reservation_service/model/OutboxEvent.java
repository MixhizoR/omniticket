package com.omniticket.reservation_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "text")
    private String payload;

    private int retryCount;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        this.status = "PENDING";
    }
}