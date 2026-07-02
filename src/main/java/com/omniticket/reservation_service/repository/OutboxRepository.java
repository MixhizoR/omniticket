package com.omniticket.reservation_service.repository;

import com.omniticket.reservation_service.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    // Sadece PENDING olanları, en eski tarihli olanlardan başlayarak çekmek için
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}