package com.group5.cats.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.group5.cats.model.NotificationOutbox;

@Service
public class SmtpEmailSender implements EmailSender {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> sender,
            @Value("${cats.mail.from:cats@localhost}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(NotificationOutbox notification) {
        JavaMailSender mailSender = sender.getIfAvailable();
        if (mailSender == null) throw new IllegalStateException("SMTP is not configured.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.getRecipientEmailSnapshot());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody());
        mailSender.send(message);
    }
}
