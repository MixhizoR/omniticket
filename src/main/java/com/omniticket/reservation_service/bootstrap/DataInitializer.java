package com.omniticket.reservation_service.bootstrap;

import java.math.BigDecimal;

import com.omniticket.reservation_service.model.Ticket;
import com.omniticket.reservation_service.model.TicketStatus;
import com.omniticket.reservation_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component // Spring'in bu sınıfı otomatik bulup çalıştırmasını sağlar
@RequiredArgsConstructor // Repository'i otomatik bağlar (Injection)
@Slf4j // Loglama yapmamızı sağlar
public class DataInitializer implements ApplicationRunner {

    private final TicketRepository ticketRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 1. Veritabanında bilet var mı kontrol et (Mükerrer veri olmasın)
        if (ticketRepository.count() == 0) {
            log.info("Veritabanı boş, örnek biletler oluşturuluyor... 🎟️");

            // 2. Örnek 5 tane bilet oluştur ve kaydet
            for (int i = 1; i <= 5; i++) {
                Ticket ticket = new Ticket();
                ticket.setSeatNumber("Sıra-A Koltuk-" + i);
                ticket.setPrice(BigDecimal.valueOf(150.0 * i));
                ticket.setStatus(TicketStatus.AVAILABLE);

                ticketRepository.save(ticket);
            }

            log.info("Başarılı! 5 adet bilet sisteme yüklendi. ✅");
        } else {
            log.info("Veritabanında zaten biletler var, yeni veri eklenmedi. ✨");
        }
    }
}