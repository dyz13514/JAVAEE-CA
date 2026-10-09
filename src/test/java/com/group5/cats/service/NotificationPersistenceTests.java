package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "cats.mail.enabled=true", "cats.mail.poll-delay-ms=600000"
})
class NotificationPersistenceTests {
    @Autowired NotificationOutboxRepository notifications;
    @Autowired EmployeeRepository employees;
    @Autowired CourseApplicationRepository applications;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired CourseApplicationService courseService;
    @Autowired EntitlementService entitlementService;

    @BeforeEach
    void clearNotifications() {
        notifications.deleteAll();
    }

    @Test
    void realBusinessTransactionsCreateSubmissionAndDecisionRecords() {
        Employee employee = employees.findByUsername("emp2").orElseThrow();
        employee.setEmail("employee@example.test");
        Employee manager = employee.getSupervisor();
        manager.setEmail("manager@example.test");
        employees.save(manager);
        employees.save(employee);
        LocalDate from = LocalDate.now().plusDays(7).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        entitlementService.setEntitlement(employee.getId(), from.getYear(), 10, 2000);
        CourseApplication application = new CourseApplication();
        application.setCourseTitle("Email integration");
        application.setCategory("EXTERNAL");
        application.setFee(100);
        application.setJustification("Learn Spring");
        application.setFromDate(from);
        application.setToDate(from.plusDays(1));
        assertNull(courseService.submitApplication(application, employee));
        assertEquals(1, notifications.count());
        assertEquals("Application approved", courseService.reviewApplication(application.getId(), manager,
                "APPROVE", "Share learnings with the team."));
        assertEquals(2, notifications.count());
        assertTrue(notifications.findAll().stream().anyMatch(record ->
                record.getNotificationType() == NotificationType.APPLICATION_APPROVED
                && record.getBody().contains("Share learnings with the team.")));
    }

    @Test
    void persistenceLeaseRecoveryDeduplicationAndRollback() {
        var transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> {
            Employee owner = employees.findByUsername("emp1").orElseThrow();
            owner.setEmail("employee@example.test");
            owner.getSupervisor().setEmail("manager@example.test");
            employees.save(owner.getSupervisor());
            employees.save(owner);
            CourseApplication application = new CourseApplication();
            application.setEmployee(owner);
            application.setCourseTitle("Transactional course");
            application.setStatus("APPLIED");
            applications.saveAndFlush(application);
            var service = new NotificationServiceImpl(notifications, true, "http://localhost:8080");
            service.createNotification(application, NotificationType.APPLICATION_SUBMITTED);
            service.createNotification(application, NotificationType.APPLICATION_SUBMITTED);
            return notifications.findAll().get(0).getId();
        });
        assertEquals(1, notifications.count());
        LocalDateTime now = LocalDateTime.now().plusSeconds(1);
        var statuses = List.of(DeliveryStatus.PENDING, DeliveryStatus.PROCESSING);
        assertEquals(List.of(id), notifications.findDueIds(statuses, now, PageRequest.of(0, 10)));
        LocalDateTime lease = now.plusMinutes(5).withNano(0);
        assertEquals(1, notifications.claim(id, statuses, DeliveryStatus.PROCESSING, now, lease));
        assertEquals(0, notifications.claim(id, statuses, DeliveryStatus.PROCESSING, now, lease));
        LocalDateTime newLease = lease.plusMinutes(5);
        assertEquals(1, notifications.claim(id, statuses, DeliveryStatus.PROCESSING, lease.plusSeconds(1), newLease));
        assertEquals(0, notifications.finish(id, DeliveryStatus.PROCESSING, lease,
                DeliveryStatus.SENT, 1, null, null, now));
        assertEquals(1, notifications.finish(id, DeliveryStatus.PROCESSING, newLease,
                DeliveryStatus.SENT, 1, null, null, now));
        assertEquals(DeliveryStatus.SENT, notifications.findById(id).orElseThrow().getDeliveryStatus());

        long count = notifications.count();
        transactions.executeWithoutResult(status -> {
            CourseApplication application = new CourseApplication();
            application.setEmployee(employees.findByUsername("emp1").orElseThrow());
            application.setStatus("APPLIED");
            applications.saveAndFlush(application);
            new NotificationServiceImpl(notifications, true, "http://localhost")
                    .createNotification(application, NotificationType.APPLICATION_SUBMITTED);
            status.setRollbackOnly();
        });
        assertEquals(count, notifications.count());
    }
}
