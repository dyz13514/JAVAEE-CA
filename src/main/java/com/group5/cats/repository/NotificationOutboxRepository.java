package com.group5.cats.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.NotificationOutbox;
import com.group5.cats.model.DeliveryStatus;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    boolean existsByDeduplicationKey(String key);
    boolean existsByRecipient_Id(Long employeeId);
    Page<NotificationOutbox> findByDeliveryStatus(DeliveryStatus status, Pageable pageable);

    @Query("select n.id from NotificationOutbox n where n.deliveryStatus in :statuses and n.nextAttemptAt <= :now order by n.id")
    List<Long> findDueIds(@Param("statuses") List<DeliveryStatus> statuses,
            @Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Transactional
    @Query("update NotificationOutbox n set n.deliveryStatus = :processing, n.nextAttemptAt = :lease where n.id = :id and n.deliveryStatus in :statuses and n.nextAttemptAt <= :now")
    int claim(@Param("id") Long id, @Param("statuses") List<DeliveryStatus> statuses,
            @Param("processing") DeliveryStatus processing, @Param("now") LocalDateTime now,
            @Param("lease") LocalDateTime lease);

    @Modifying
    @Transactional
    @Query("update NotificationOutbox n set n.deliveryStatus = :status, n.retryCount = :retries, n.nextAttemptAt = :next, n.lastError = :error, n.sentAt = :sent where n.id = :id and n.deliveryStatus = :processing and n.nextAttemptAt = :lease")
    int finish(@Param("id") Long id, @Param("processing") DeliveryStatus processing,
            @Param("lease") LocalDateTime lease, @Param("status") DeliveryStatus status,
            @Param("retries") int retries, @Param("next") LocalDateTime next,
            @Param("error") String error, @Param("sent") LocalDateTime sent);
}
