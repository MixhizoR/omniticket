package com.omniticket.reservation_service.service;

import java.math.BigDecimal;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.omniticket.reservation_service.exception.EmailSendingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTicketEmail(String to, String seat, BigDecimal price) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = String.format(
                    "<h1>Your Ticket is Ready! 🎟️</h1>" +
                            "<p>Your Seat Number: <b>%s</b></p>" +
                            "<p>Amount Paid: <b>%.2f TL</b></p>" +
                            "<br><p>Thank you for choosing OmniTicket!</p>",
                    seat, price);

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("Ticket Confirmation - OmniTicket");
            helper.setFrom("no-reply@omniticket.com");

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Error sending email: {}", e.getMessage());
            throw new EmailSendingException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
