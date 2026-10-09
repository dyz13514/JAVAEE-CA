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
    private PublicHolidayRepository holidays;
    private CourseApplicationServiceImpl service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        applications = mock(CourseApplicationRepository.class);
        entitlements = mock(EntitlementService.class);
        holidays = mock(PublicHolidayRepository.class);
        service = new CourseApplicationServiceImpl(applications, mock(EmployeeRepository.class), entitlements, holidays);
        employee = new Employee();
        employee.setId(1L);
        when(applications.findByEmployee(employee)).thenReturn(List.of());
        when(holidays.findByHolidayDate(any())).thenReturn(Optional.empty());
        when(entitlements.findEntitlement(eq(1L), anyInt())).thenAnswer(invocation ->
                Optional.of(new AnnualEntitlement(employee, invocation.getArgument(1), 10, 2000)));
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
}
