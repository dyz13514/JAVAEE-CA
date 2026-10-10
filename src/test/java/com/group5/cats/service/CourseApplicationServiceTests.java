package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.group5.cats.model.*;
import com.group5.cats.repository.*;

class CourseApplicationServiceTests {
    private CourseApplicationRepository applications;
    private EntitlementService entitlements;
    private CategoryRepository categories;
    private PublicHolidayRepository holidays;
    private CourseApplicationServiceImpl service;
    private NotificationService notifications;
    private Employee employee;
    private EmployeeRepository employees;

    @BeforeEach
    void setUp() {
        applications = mock(CourseApplicationRepository.class);
        entitlements = mock(EntitlementService.class);
        holidays = mock(PublicHolidayRepository.class);
        notifications = mock(NotificationService.class);
        categories = mock(CategoryRepository.class);
        when(categories.findById(anyLong())).thenAnswer(invocation -> {
            long id = invocation.getArgument(0);
            return id >= 1 && id <= 4 ? Optional.of(com.group5.cats.CategoryFixtures.category(
                    id == 1 ? "INTERNAL" : id == 2 ? "EXTERNAL" : id == 3 ? "CERTIFICATION" : "WORKSHOP")) : Optional.empty();
        });
        employees = mock(EmployeeRepository.class);
        service = new CourseApplicationServiceImpl(applications, employees,
                entitlements, holidays, notifications, categories);
        employee = new Employee();
        employee.setId(1L);
        Employee manager = new Employee();
        manager.setId(2L);
        manager.setRole(EmployeeRole.MANAGER);
        employee.setSupervisor(manager);
        when(applications.findByEmployee(employee)).thenReturn(List.of());
        when(holidays.findByHolidayDate(any())).thenReturn(Optional.empty());
        when(entitlements.findEntitlement(eq(1L), anyInt())).thenAnswer(invocation ->
                Optional.of(new AnnualEntitlement(employee, invocation.getArgument(1), 10, 2000)));
    }

    private CourseApplication validApplication() {
        CourseApplication application = new CourseApplication();
        application.setCourseTitle("Spring MVC");
        application.setCategory(com.group5.cats.CategoryFixtures.category("EXTERNAL"));
        application.setJustification("Improve our application maintenance.");
        LocalDate from = LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        application.setFromDate(from);
        application.setToDate(from.plusDays(1));
        application.setFee(100);
        return application;
    }

    @Test
    void successfulSubmissionCreatesNotificationButInvalidSubmissionDoesNot() {
        CourseApplication application = validApplication();
        assertNull(service.submitApplication(application, employee));
        verify(notifications).createNotification(application, NotificationType.APPLICATION_SUBMITTED);
        reset(notifications);
        application.setCourseTitle(" ");
        assertNotNull(service.submitApplication(application, employee));
        verifyNoInteractions(notifications);
    }

    @Test
    void onlyValidManagerDecisionCreatesNotification() {
        for (String decision : List.of("APPROVE", "REJECT")) {
            Employee manager = new Employee();
            manager.setId(2L);
            manager.setRole(EmployeeRole.MANAGER);
            when(employees.findById(2L)).thenReturn(Optional.of(manager));
            employee.setSupervisor(manager);
            CourseApplication application = validApplication();
            application.setEmployee(employee);
            application.setStatus("APPLIED");
            when(applications.findById(10L)).thenReturn(Optional.of(application));
            assertNotNull(service.reviewApplication(10L, manager, decision, "Relevant training"));
            verify(notifications).createNotification(application, decision.equals("APPROVE")
                    ? NotificationType.APPLICATION_APPROVED : NotificationType.APPLICATION_REJECTED);
            reset(notifications);
            service.reviewApplication(10L, manager, decision, "Relevant training");
            verifyNoInteractions(notifications);
        }
    }

    private void allowSubmission(CourseApplication application) {
        when(applications.findByEmployee(employee)).thenReturn(List.of());
        when(holidays.findByHolidayDate(any())).thenReturn(Optional.empty());
        when(entitlements.findEntitlement(1L, application.getFromDate().getYear()))
                .thenReturn(Optional.of(new AnnualEntitlement(employee, application.getFromDate().getYear(), 10, 2000)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"courseTitle", "justification"})
    void missingRequiredTextCannotBeSaved(String field) {
        for (String value : new String[] {null, "", " \t "}) {
            CourseApplication application = validApplication();
            if (field.equals("courseTitle")) application.setCourseTitle(value);
            else application.setJustification(value);
            assertNotNull(service.submitApplication(application, employee));
        }
        verify(applications, never()).save(any());
    }

    @Test
    void invalidCategoryCannotBeSaved() {
        for (Long id : new Long[] {null, 0L, 999L}) {
            CourseApplication application = validApplication();
            application.setCategory(null);
            application.setCategoryId(id);
            assertNotNull(service.submitApplication(application, employee));
        }
        verify(applications, never()).save(any());
    }

    static Stream<Double> invalidAmounts() {
        return Stream.of(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void invalidFeeCannotBeSaved(double fee) {
        CourseApplication application = validApplication();
        application.setFee(fee);
        assertNotNull(service.submitApplication(application, employee));
        verify(applications, never()).save(any());
    }

    @Test
    void internalFeeIsSavedButDoesNotUseBudget() {
        CourseApplication application = validApplication();
        application.setCategory(com.group5.cats.CategoryFixtures.category("INTERNAL"));
        application.setFee(9000);
        when(entitlements.findEntitlement(eq(1L), anyInt())).thenAnswer(invocation ->
                Optional.of(new AnnualEntitlement(employee, invocation.getArgument(1), 10, 0)));
        assertNull(service.submitApplication(application, employee));
        assertEquals(9000, application.getFee());
        assertEquals(2, application.getTrainingDays());
        verify(applications).save(application);
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "justification", "provider", "dissemination"})
    void textExceedingStorageLimitCannotBeSaved(String field) {
        CourseApplication application = validApplication();
        String text = "x".repeat(256);
        switch (field) {
            case "title" -> application.setCourseTitle(text);
            case "justification" -> application.setJustification(text);
            case "provider" -> application.setProvider(text);
            case "dissemination" -> application.setDissemination(text);
        }
        assertNotNull(service.submitApplication(application, employee));
        verify(applications, never()).save(any());
    }

    @Test
    void newSubmissionCannotOverwriteExistingRecord() {
        CourseApplication application = validApplication();
        application.setId(99L);
        assertNotNull(service.submitApplication(application, employee));
        verify(applications, never()).save(any());
    }

    @Test
    void validSubmissionNormalizesTextAndComputesDays() {
        CourseApplication application = validApplication();
        application.setCourseTitle("  Spring MVC  ");
        allowSubmission(application);
        assertNull(service.submitApplication(application, employee));
        assertEquals("Spring MVC", application.getCourseTitle());
        assertEquals(2.0, application.getTrainingDays());
        assertEquals("APPLIED", application.getStatus());
        verify(applications).save(application);
    }

    @Test
    void validInternalHalfDayRemainsSupported() {
        CourseApplication application = validApplication();
        application.setCategory(com.group5.cats.CategoryFixtures.category("INTERNAL"));
        application.setFee(0);
        application.setHalfDay(true);
        application.setToDate(application.getFromDate());
        allowSubmission(application);
        assertNull(service.submitApplication(application, employee));
        assertEquals(0.5, application.getTrainingDays());
    }

    @Test
    void publicHolidayCannotBeUsedForHalfDay() {
        CourseApplication application = validApplication();
        application.setCategory(com.group5.cats.CategoryFixtures.category("INTERNAL"));
        application.setFee(0);
        application.setHalfDay(true);
        application.setToDate(application.getFromDate());
        when(holidays.findByHolidayDate(application.getFromDate()))
                .thenReturn(Optional.of(new PublicHoliday(application.getFromDate(), "Holiday")));
        assertNotNull(service.submitApplication(application, employee));
        verify(applications, never()).save(any());
    }

    @Test
    void invalidUpdatePreservesOriginalRecord() {
        CourseApplication existing = validApplication();
        existing.setId(10L);
        existing.setEmployee(employee);
        when(applications.findById(10L)).thenReturn(Optional.of(existing));
        CourseApplication update = validApplication();
        update.setJustification(" ");
        assertNotNull(service.updateApplication(10L, update, employee));
        assertEquals("Improve our application maintenance.", existing.getJustification());
        assertEquals("APPLIED", existing.getStatus());
        verify(applications, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t "})
    void completionRequiresFeedback(String feedback) {
        CourseApplication application = validApplication();
        application.setEmployee(employee);
        application.setStatus("APPROVED");
        application.setToDate(LocalDate.now().minusDays(1));
        when(applications.findById(10L)).thenReturn(Optional.of(application));
        assertNotEquals("Course application completed successfully", service.completeApplication(10L, employee, feedback));
        assertEquals("APPROVED", application.getStatus());
        verify(applications, never()).save(any());
    }

    @Test
    void completionStoresTrimmedFeedback() {
        CourseApplication application = validApplication();
        application.setEmployee(employee);
        application.setStatus("APPROVED");
        application.setToDate(LocalDate.now().minusDays(1));
        when(applications.findById(10L)).thenReturn(Optional.of(application));
        assertEquals("Course application completed successfully", service.completeApplication(10L, employee, " Learned Spring. "));
        assertEquals("Learned Spring.", application.getExperienceComments());
        assertEquals("COMPLETED", application.getStatus());
    }
    @Test
    void staffAndManagersCanApplyButAdminsCannotAndSelfApprovalIsBlocked() {
        for (EmployeeRole role : EmployeeRole.values()) {
            employee.setRole(role);
            CourseApplication data = validApplication();
            if (role == EmployeeRole.ADMIN) {
                assertTrue(service.submitApplication(data, employee).contains("Administrators"));
            } else {
                assertNull(service.submitApplication(data, employee));
            }
        }
        employee.setRole(EmployeeRole.MANAGER);
        employee.setSupervisor(employee);
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        CourseApplication application = validApplication();
        application.setEmployee(employee);
        when(applications.findById(99L)).thenReturn(Optional.of(application));
        assertTrue(service.reviewApplication(99L, employee, "APPROVE", "Self review").contains("Self-approval"));
        assertEquals("APPLIED", application.getStatus());
    }

    @Test
    void internalFeeStillCannotBypassDayQuota() {
        CourseApplication data = validApplication();
        data.setCategory(com.group5.cats.CategoryFixtures.category("INTERNAL"));
        data.setFee(9000);
        when(entitlements.findEntitlement(eq(1L), anyInt())).thenAnswer(invocation ->
                Optional.of(new AnnualEntitlement(employee, invocation.getArgument(1), 1, 0)));
        assertTrue(service.submitApplication(data, employee).contains("Training days quota exceeded"));
        verify(applications, never()).save(any());
    }

    @Test
    void newCategoryUsesBudgetAndItsHalfDayPermission() {
        Category workshop = com.group5.cats.CategoryFixtures.category("WORKSHOP");
        workshop.setHalfDayAllowed(true);
        when(categories.findById(4L)).thenReturn(Optional.of(workshop));
        CourseApplication data = validApplication();
        data.setCategoryId(4L);
        data.setHalfDay(true);
        data.setToDate(data.getFromDate());
        assertNull(service.submitApplication(data, employee));
        assertEquals(0.5, data.getTrainingDays());
        data.setFee(3000);
        assertTrue(service.submitApplication(data, employee).contains("Training budget exceeded"));
    }

    @Test
    void editRecomputesDaysAndExcludesOwnReservation() {
        CourseApplication saved = validApplication();
        saved.setId(9L);
        saved.setEmployee(employee);
        saved.setTrainingDays(2);
        when(applications.findById(9L)).thenReturn(Optional.of(saved));
        when(applications.findByEmployee(employee)).thenReturn(List.of(saved));
        CourseApplication updated = validApplication();
        updated.setToDate(updated.getFromDate().plusDays(3));
        when(entitlements.findEntitlement(eq(1L), anyInt())).thenAnswer(invocation ->
                Optional.of(new AnnualEntitlement(employee, invocation.getArgument(1), 4, 100)));
        assertNull(service.updateApplication(9L, updated, employee));
        assertEquals(4, saved.getTrainingDays());
        assertEquals("UPDATED", saved.getStatus());
    }

    @Test
    void interiorHolidayAndWeekendDoNotCountAsTrainingDays() {
        CourseApplication data = validApplication();
        data.setToDate(data.getFromDate().plusDays(7));
        when(holidays.findByHolidayDate(data.getFromDate().plusDays(1)))
                .thenReturn(Optional.of(new PublicHoliday(data.getFromDate().plusDays(1), "Test holiday")));
        assertNull(service.submitApplication(data, employee));
        assertEquals(5, data.getTrainingDays());
    }

    @Test
    void disallowedHalfDayAndMissingSupervisorAreRejected() {
        CourseApplication data = validApplication();
        data.setHalfDay(true);
        data.setToDate(data.getFromDate());
        assertNotNull(service.submitApplication(data, employee));
        employee.setSupervisor(null);
        assertTrue(service.submitApplication(validApplication(), employee).contains("assign your manager"));
    }
}
