package com.group5.cats.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.group5.cats.model.*;
import com.group5.cats.repository.NotificationOutboxRepository;
import com.group5.cats.service.EmailSender;

@Component
@ConditionalOnProperty(name = "cats.mail.enabled", havingValue = "true")
public class NotificationDispatcher {
    private static final List<DeliveryStatus> SENDABLE = List.of(DeliveryStatus.PENDING, DeliveryStatus.PROCESSING);
    private final NotificationOutboxRepository repository;
    private final EmailSender sender;
    private final int maxAttempts;

    public NotificationDispatcher(NotificationOutboxRepository repository, EmailSender sender,
            @Value("${cats.mail.max-attempts:3}") int maxAttempts) {
        this.repository = repository;
        this.sender = sender;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${cats.mail.poll-delay-ms:10000}", initialDelayString = "${cats.mail.poll-delay-ms:10000}")
    public void dispatch() {
        for (Long id : repository.findDueIds(SENDABLE, LocalDateTime.now(), PageRequest.of(0, 10))) {
            sendOne(id);
        }
    }

    public void sendOne(Long id) {
        LocalDateTime lease = LocalDateTime.now().plusMinutes(5).withNano(0);
        if (repository.claim(id, SENDABLE, DeliveryStatus.PROCESSING, LocalDateTime.now(), lease) != 1) return;
        NotificationOutbox notification = repository.findById(id).orElseThrow();
        int attempts = notification.getRetryCount() + 1;
        try {
            sender.send(notification);
            repository.finish(id, DeliveryStatus.PROCESSING, lease, DeliveryStatus.SENT,
                    attempts, null, null, LocalDateTime.now());
        } catch (RuntimeException exception) {
            // Store a generic error: SMTP exception messages may contain credentials or server data.
            DeliveryStatus status = attempts >= maxAttempts ? DeliveryStatus.FAILED : DeliveryStatus.PENDING;
            LocalDateTime next = status == DeliveryStatus.FAILED ? null : LocalDateTime.now().plusSeconds(60L * attempts);
            repository.finish(id, DeliveryStatus.PROCESSING, lease, status, attempts, next,
                    "Email delivery failed (" + exception.getClass().getSimpleName() + "). Check SMTP configuration.", null);
        }
    }
}
