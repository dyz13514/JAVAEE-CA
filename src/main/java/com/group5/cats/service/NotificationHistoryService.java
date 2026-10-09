package com.group5.cats.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.DeliveryStatus;
import com.group5.cats.model.NotificationOutbox;
import com.group5.cats.repository.NotificationOutboxRepository;

@Service
public class NotificationHistoryService {
    private final NotificationOutboxRepository repository;

    public NotificationHistoryService(NotificationOutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationOutbox> findNotifications(DeliveryStatus status, int page) {
        var pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return status == null ? repository.findAll(pageable) : repository.findByDeliveryStatus(status, pageable);
    }
}
