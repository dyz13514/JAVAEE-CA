package com.group5.cats.service;
import com.group5.cats.model.NotificationOutbox;
public interface EmailSender {
    void send(NotificationOutbox notification);
}
