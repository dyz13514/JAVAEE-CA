package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.group5.cats.model.*;
import com.group5.cats.repository.NotificationOutboxRepository;
import com.group5.cats.scheduler.NotificationDispatcher;

class NotificationTests {
    private CourseApplication application() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setName("Ben");
        employee.setEmail("ben@example.test");
        Employee manager = new Employee();
        manager.setEmail("manager@example.test");
        employee.setSupervisor(manager);
        CourseApplication application = new CourseApplication();
        application.setId(10L);
        application.setEmployee(employee);
        application.setCourseTitle("Spring");
        application.setStatus("APPLIED");
        application.setManagerComment("Relevant to your work.");
        return application;
    }

    @Test
    void submissionUsesSupervisorAndSnapshotsAddress() {
        var repository = mock(NotificationOutboxRepository.class);
        var service = new NotificationServiceImpl(repository, true, "http://localhost:8080/");
        service.createNotification(application(), NotificationType.APPLICATION_SUBMITTED);
        var record = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(record.capture());
        assertEquals("manager@example.test", record.getValue().getRecipientEmailSnapshot());
        assertEquals(DeliveryStatus.PENDING, record.getValue().getDeliveryStatus());
        assertTrue(record.getValue().getBody().contains("http://localhost:8080/employee/login"));
        assertEquals("10:APPLICATION_SUBMITTED", record.getValue().getDeduplicationKey());
    }

    @Test
    void bothDecisionsNotifyEmployeeWithReason() {
        for (NotificationType type : new NotificationType[] {
                NotificationType.APPLICATION_APPROVED, NotificationType.APPLICATION_REJECTED}) {
            var repository = mock(NotificationOutboxRepository.class);
            new NotificationServiceImpl(repository, true, "http://localhost:8080").createNotification(application(), type);
            var record = ArgumentCaptor.forClass(NotificationOutbox.class);
            verify(repository).save(record.capture());
            assertEquals("ben@example.test", record.getValue().getRecipientEmailSnapshot());
            assertTrue(record.getValue().getBody().contains("Relevant to your work."));
        }
    }

    @Test
    void disabledAndDuplicateNotificationsAreNotCreated() {
        var repository = mock(NotificationOutboxRepository.class);
        new NotificationServiceImpl(repository, false, "http://localhost").createNotification(application(), NotificationType.APPLICATION_SUBMITTED);
        verifyNoInteractions(repository);
        when(repository.existsByDeduplicationKey(anyString())).thenReturn(true);
        new NotificationServiceImpl(repository, true, "http://localhost").createNotification(application(), NotificationType.APPLICATION_SUBMITTED);
        verify(repository, never()).save(any());
    }

    @Test
    void missingRecipientDoesNotThrowAndRecordsFailure() {
        var repository = mock(NotificationOutboxRepository.class);
        var application = application();
        application.getEmployee().setSupervisor(null);
        new NotificationServiceImpl(repository, true, "http://localhost").createNotification(application, NotificationType.APPLICATION_SUBMITTED);
        var record = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repository).save(record.capture());
        assertEquals(DeliveryStatus.FAILED, record.getValue().getDeliveryStatus());
        assertNull(record.getValue().getNextAttemptAt());
    }

    @Test
    void failedSendRetriesThenStopsAndDoesNotStoreSensitiveError() {
        var repository = mock(NotificationOutboxRepository.class);
        var sender = mock(EmailSender.class);
        var dispatcher = new NotificationDispatcher(repository, sender, 3);
        var record = new NotificationOutbox();
        record.setRetryCount(0);
        when(repository.claim(anyLong(), anyList(), any(), any(), any())).thenReturn(1);
        when(repository.findById(10L)).thenReturn(Optional.of(record));
        doThrow(new IllegalStateException("password=secret")).when(sender).send(record);
        dispatcher.sendOne(10L);
        verify(repository).finish(eq(10L), eq(DeliveryStatus.PROCESSING), any(), eq(DeliveryStatus.PENDING),
                eq(1), any(LocalDateTime.class), argThat(value -> !value.contains("secret")), isNull());
        record.setRetryCount(2);
        dispatcher.sendOne(10L);
        verify(repository).finish(eq(10L), eq(DeliveryStatus.PROCESSING), any(), eq(DeliveryStatus.FAILED),
                eq(3), isNull(), anyString(), isNull());
    }

    @Test
    void onlyClaimWinnerSendsAndSuccessClearsFailure() {
        var repository = mock(NotificationOutboxRepository.class);
        var sender = mock(EmailSender.class);
        var dispatcher = new NotificationDispatcher(repository, sender, 3);
        dispatcher.sendOne(10L);
        verifyNoInteractions(sender);
        var record = new NotificationOutbox();
        when(repository.claim(anyLong(), anyList(), any(), any(), any())).thenReturn(1);
        when(repository.findById(10L)).thenReturn(Optional.of(record));
        dispatcher.sendOne(10L);
        verify(sender).send(record);
        verify(repository).finish(eq(10L), eq(DeliveryStatus.PROCESSING), any(), eq(DeliveryStatus.SENT),
                eq(1), isNull(), isNull(), any(LocalDateTime.class));
    }
}
