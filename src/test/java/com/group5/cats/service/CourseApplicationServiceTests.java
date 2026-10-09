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
    private CourseRepository courses;
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
        courses = mock(CourseRepository.class);
        employees = mock(EmployeeRepository.class);
        service = new CourseApplicationServiceImpl(applications, employees,
                entitlements, holidays, notifications, courses, new CourseScheduleServiceImpl(holidays));
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

    @Test
    void selectingCourseUsesServerDetailsAndKeepsActualFee() {
        Course course = recordedCourse();
        CourseApplication data = validApplication();
        data.setCourseId(course.getId());
        data.setCourseTitle("Tampered title");
        data.setProvider("Tampered provider");
        data.setCategory("INTERNAL");
        assertNull(service.submitApplication(data, employee));
        assertSame(course, data.getCourse());
        assertEquals("Recorded course", data.getCourseTitle());
        verify(notifications).createNotification(data, NotificationType.APPLICATION_SUBMITTED);
        assertEquals("Original provider", data.getProvider());
        assertEquals("EXTERNAL", data.getCategory());
        assertEquals(100, data.getFee());
    }

    @Test
    void changingCatalogueDoesNotRewriteApplicationSnapshotOnEdit() {
        Course course = recordedCourse();
        CourseApplication saved = validApplication();
        saved.setId(9L);
        saved.setEmployee(employee);
        saved.setCourse(course);
        saved.setCourseTitle("Original title");
        saved.setProvider("Original provider");
        when(applications.findById(9L)).thenReturn(Optional.of(saved));
        course.setTitle("Renamed course");
        course.setCategory("CERTIFICATION");
        course.getProvider().setName("Renamed provider");
        course.setFee(999);
        CourseApplication update = validApplication();
        update.setCourseId(course.getId());
        assertNull(service.updateApplication(9L, update, employee));
        assertEquals("Original title", saved.getCourseTitle());
        assertEquals("Original provider", saved.getProvider());
        assertEquals("EXTERNAL", saved.getCategory());
        assertEquals(100, saved.getFee());
    }

    @Test
    void missingSelectedCourseCannotBeSubmitted() {
        CourseApplication data = validApplication();
        data.setCourseId(999L);
        assertNotNull(service.submitApplication(data, employee));
        verify(applications, never()).save(any());
    }

    @Test
    void scheduledApplicationCannotSwitchToManualDates() {
        CourseApplication saved = validApplication();
        saved.setId(9L);
        saved.setEmployee(employee);
        saved.setCourse(recordedCourse());
        when(applications.findById(9L)).thenReturn(Optional.of(saved));
        CourseApplication manual = validApplication();
        assertNotNull(service.updateApplication(9L, manual, employee));
        assertNotNull(saved.getCourse());
        verify(applications, never()).save(any());
    }

    private Course recordedCourse() {
        TrainingProvider provider = new TrainingProvider("Original provider");
        Course course = new Course("Recorded course", "EXTERNAL", provider, 500);
        course.setId(10L);
        when(courses.findById(10L)).thenReturn(Optional.of(course));
        course.setDurationDays(3);
        course.getStartDates().add(validApplication().getFromDate());
        return course;
    }

    private CourseApplication validApplication() {
        CourseApplication application = new CourseApplication();
        application.setCourseTitle("Spring MVC");
        application.setCategory("EXTERNAL");
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "UNKNOWN"})
    void invalidCategoryCannotBeSaved(String category) {
        CourseApplication application = validApplication();
        application.setCategory(category);
        assertNotNull(service.submitApplication(application, employee));
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
    void internalTrainingCannotChargeFee() {
        CourseApplication application = validApplication();
        application.setCategory("INTERNAL");
        assertNotNull(service.submitApplication(application, employee));
        verify(applications, never()).save(any());
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
        application.setCategory("INTERNAL");
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
        application.setCategory("INTERNAL");
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
    void scheduledApplicationIgnoresTamperedEndDateAndHalfDay() {
        Course course = recordedCourse();
        CourseApplication data = validApplication();
        data.setCourseId(course.getId());
        data.setToDate(data.getFromDate().plusDays(20));
        data.setHalfDay(true);
        assertNull(service.submitApplication(data, employee));
        assertEquals(data.getFromDate().plusDays(2), data.getToDate());
        assertEquals(3, data.getTrainingDays());
        assertFalse(data.getHalfDay());
    }

    @Test
    void unpublishedStartDateAndMissingManagerCannotBeSubmitted() {
        Course course = recordedCourse();
        CourseApplication data = validApplication();
        data.setCourseId(course.getId());
        data.setFromDate(data.getFromDate().plusDays(1));
        assertNotNull(service.submitApplication(data, employee));
        employee.setSupervisor(null);
        assertTrue(service.submitApplication(validApplication(), employee).contains("assign your manager"));
        verify(applications, never()).save(any());
    }

    @Test
    void applicantsOfEveryRoleNeedAnotherManagerAndCannotApproveThemselves() {
        for (EmployeeRole role : EmployeeRole.values()) {
            employee.setRole(role);
            Course course = recordedCourse();
            CourseApplication data = validApplication();
            data.setCourseId(course.getId());
            assertNull(service.submitApplication(data, employee));
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

}
