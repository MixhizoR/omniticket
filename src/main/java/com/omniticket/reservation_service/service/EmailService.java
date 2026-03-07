package com.omniticket.reservation_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTicketEmail(String to, String seat, Double price) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlMsg = String.format(
                "<h1>Biletiniz Hazır! 🎟️</h1>" +
                        "<p>Koltuk Numaranız: <b>%s</b></p>" +
                        "<p>Ödenen Tutar: <b>%.2f TL</b></p>" +
                        "<br><p>OmniTicket'ı tercih ettiğiniz için teşekkürler!</p>",
                seat, price);

        helper.setText(htmlMsg, true);
        helper.setTo(to);
        helper.setSubject("Bilet Onayı - OmniTicket");
        helper.setFrom("no-reply@omniticket.com");

        mailSender.send(mimeMessage);
    }
}