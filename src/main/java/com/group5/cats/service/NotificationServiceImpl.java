package com.group5.cats.service;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.group5.cats.model.*;
import com.group5.cats.repository.NotificationOutboxRepository;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationOutboxRepository repository;
    private final boolean enabled;
    private final String baseUrl;

    public NotificationServiceImpl(NotificationOutboxRepository repository,
            @Value("${cats.mail.enabled:false}") boolean enabled,
            @Value("${cats.mail.base-url:http://localhost:8080}") String baseUrl) {
        this.repository = repository;
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public void createNotification(CourseApplication application, NotificationType type) {
        if (!enabled) return;
        String key = application.getId() + ":" + type.name();
        if (repository.existsByDeduplicationKey(key)) return;
        Employee owner = application.getEmployee();
        Employee recipient = type == NotificationType.APPLICATION_SUBMITTED ? owner.getSupervisor() : owner;
        String email = recipient == null ? null : recipient.getEmail();
        NotificationOutbox notification = new NotificationOutbox();
        notification.setApplication(application);
        notification.setRecipient(recipient);
        notification.setRecipientEmailSnapshot(email);
        notification.setNotificationType(type);
        notification.setDeduplicationKey(key);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRetryCount(0);
        notification.setSubject("CATS: " + type.name().replace('_', ' ') + " #" + application.getId());
        String body = "Applicant: " + owner.getName() + "\nCourse: " + application.getCourseTitle()
                + "\nPeriod: " + application.getFromDate() + " to " + application.getToDate()
                + "\nApplication: #" + application.getId() + "\nStatus: " + application.getStatus();
        if (type != NotificationType.APPLICATION_SUBMITTED) {
            body += "\nManager reason: " + application.getManagerComment();
        }
        notification.setBody(body + "\n\nLog in to view the application and comments: " + baseUrl + "/employee/login");
        if (email == null || email.isBlank()) {
            notification.setDeliveryStatus(DeliveryStatus.FAILED);
            notification.setLastError("Recipient or recipient email is not configured.");
        } else {
            notification.setDeliveryStatus(DeliveryStatus.PENDING);
            notification.setNextAttemptAt(LocalDateTime.now());
        }
        repository.save(notification);
    }
}
