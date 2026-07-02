package com.omniticket.reservation_service.service;

import java.math.BigDecimal;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void givenValidEmailParams_whenSendTicketEmail_thenEmailSent() throws MessagingException {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendTicketEmail("test@example.com", "A1", BigDecimal.valueOf(100.0));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void givenMessagingException_whenSendTicketEmail_thenThrowsRuntimeException() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(MessagingException.class,
                () -> emailService.sendTicketEmail("test@example.com", "A1", BigDecimal.valueOf(100.0)));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}