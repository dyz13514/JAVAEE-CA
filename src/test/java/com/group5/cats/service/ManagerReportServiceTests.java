package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.AttendanceReportRow;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.BudgetReportRow;
import com.group5.cats.dto.CourseCategory;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.EntitlementSummary;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;

class ManagerReportServiceTests {

    private static final LocalDate PERIOD_FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate PERIOD_TO = LocalDate.of(2026, 3, 31);

    private EmployeeRepository employeeRepository;
    private CourseApplicationRepository courseApplicationRepository;
    private EntitlementService entitlementService;
    private ManagerReportServiceImpl service;

    private Employee manager;
    private Employee ben;
    private Employee cathy;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        courseApplicationRepository = mock(CourseApplicationRepository.class);
        entitlementService = mock(EntitlementService.class);
        service = new ManagerReportServiceImpl(
                employeeRepository, courseApplicationRepository, entitlementService);

        manager = employee(1L, "Alice Wong", EmployeeRole.MANAGER);
        ben = subordinate(2L, "Ben Tan", manager);
        cathy = subordinate(3L, "Cathy Lim", manager);
        when(employeeRepository.findBySupervisor_Id(1L)).thenReturn(List.of(ben, cathy));
    }

    @Test
    void onlyApprovedAndCompletedApplicationsCountAsConfirmedAttendance() {
        when(courseApplicationRepository.findByEmployeeIn(List.of(ben, cathy))).thenReturn(List.of(
                application(ben, "Approved course", CourseCategory.EXTERNAL, "APPROVED", 100),
                application(ben, "Completed course", CourseCategory.EXTERNAL, "COMPLETED", 200),
                application(ben, "Pending course", CourseCategory.EXTERNAL, "APPLIED", 300),
                application(ben, "Updated course", CourseCategory.EXTERNAL, "UPDATED", 400),
                application(cathy, "Rejected course", CourseCategory.EXTERNAL, "REJECTED", 500),
                application(cathy, "Withdrawn course", CourseCategory.EXTERNAL, "DELETED", 600),
                application(cathy, "Cancelled course", CourseCategory.EXTERNAL, "CANCELLED", 700)));

        AttendanceReport report = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.ALL);

        assertEquals(2, report.getRecordCount());
        assertTrue(report.getRows().stream().allMatch(row ->
                "APPROVED".equals(row.getStatus()) || "COMPLETED".equals(row.getStatus())));
        assertEquals(300, report.getTotalFee());
    }

    @Test
    void attendanceReportFiltersByCourseCategory() {
        when(courseApplicationRepository.findByEmployeeIn(List.of(ben, cathy))).thenReturn(List.of(
                application(ben, "Internal course", CourseCategory.INTERNAL, "APPROVED", 0),
                application(ben, "External course", CourseCategory.EXTERNAL, "APPROVED", 500),
                application(ben, "Certification", CourseCategory.CERTIFICATION, "APPROVED", 900)));

        AttendanceReport internal = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.INTERNAL);
        AttendanceReport external = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.EXTERNAL);
        AttendanceReport all = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.ALL);

        assertEquals(List.of("Internal course"), titles(internal));
        assertEquals(List.of("External course"), titles(external));
        assertEquals(3, all.getRecordCount());
        // Only the external course and the certification count toward the budget.
        assertEquals(1400, all.getBudgetRelevantFee());
        assertEquals(1400, all.getTotalFee());
    }

    @Test
    void attendanceReportIncludesCoursesOverlappingTheReportingPeriod() {
        when(courseApplicationRepository.findByEmployeeIn(List.of(ben, cathy))).thenReturn(List.of(
                application(ben, "Starts before", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 2, 20), LocalDate.of(2026, 3, 5)),
                application(ben, "Ends after", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 3, 25), LocalDate.of(2026, 4, 10)),
                application(ben, "Touches first day", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1)),
                application(ben, "Touches last day", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 3)),
                application(ben, "Entirely before", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 27)),
                application(ben, "Entirely after", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2))));

        AttendanceReport report = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.ALL);

        assertEquals(List.of("Touches first day", "Starts before", "Ends after", "Touches last day"),
                titles(report));
    }

    @Test
    void reportsCoverAllDirectReportsOrOneSelectedEmployee() {
        when(courseApplicationRepository.findByEmployeeIn(List.of(ben, cathy))).thenReturn(List.of(
                application(ben, "Ben course", CourseCategory.EXTERNAL, "APPROVED", 100),
                application(cathy, "Cathy course", CourseCategory.EXTERNAL, "APPROVED", 200)));
        when(courseApplicationRepository.findByEmployeeIn(List.of(ben))).thenReturn(List.of(
                application(ben, "Ben course", CourseCategory.EXTERNAL, "APPROVED", 100)));

        AttendanceReport everyone = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.ALL);
        AttendanceReport justBen = service.findAttendanceReport(
                manager, ben.getId(), PERIOD_FROM, PERIOD_TO, CourseCategory.ALL);

        assertEquals(2, everyone.getRecordCount());
        assertEquals(List.of("Ben course"), titles(justBen));
        assertEquals(List.of("Ben Tan", "Cathy Lim"),
                service.findReportableEmployees(manager).stream().map(Employee::getName).toList());
    }

    @Test
    void employeesOutsideTheManagersTeamAreNeverReported() {
        Employee outsider = employee(99L, "Someone Else", EmployeeRole.REGULAR_STAFF);

        assertThrows(SecurityException.class, () -> service.findAttendanceReport(
                manager, outsider.getId(), PERIOD_FROM, PERIOD_TO, CourseCategory.ALL));
        assertThrows(SecurityException.class, () -> service.findAttendanceReport(
                manager, manager.getId(), PERIOD_FROM, PERIOD_TO, CourseCategory.ALL));
        assertThrows(SecurityException.class, () -> service.findBudgetReport(
                manager, outsider.getId(), 2026));
        verifyNoInteractions(courseApplicationRepository);
    }

    @Test
    void invalidPeriodCategoryYearAndEmployeeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.findAttendanceReport(
                manager, null, PERIOD_TO, PERIOD_FROM, CourseCategory.ALL));
        assertThrows(IllegalArgumentException.class, () -> service.findAttendanceReport(
                manager, null, null, PERIOD_TO, CourseCategory.ALL));
        assertThrows(IllegalArgumentException.class, () -> service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, "NOT_A_CATEGORY"));
        assertThrows(IllegalArgumentException.class, () -> service.findAttendanceReport(
                manager, 0L, PERIOD_FROM, PERIOD_TO, CourseCategory.ALL));
        assertThrows(IllegalArgumentException.class, () -> service.findBudgetReport(manager, null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.findBudgetReport(manager, null, 10000));
        assertThrows(IllegalArgumentException.class, () -> service.findBudgetReport(manager, null, null));
    }

    @Test
    void aManagerWithoutDirectReportsGetsEmptyReportsWithoutQueryingCourses() {
        when(employeeRepository.findBySupervisor_Id(1L)).thenReturn(List.of());

        AttendanceReport attendance = service.findAttendanceReport(
                manager, null, PERIOD_FROM, PERIOD_TO, CourseCategory.ALL);
        BudgetReport budget = service.findBudgetReport(manager, null, 2026);

        assertTrue(attendance.getRows().isEmpty());
        assertTrue(budget.getRows().isEmpty());
        verifyNoInteractions(courseApplicationRepository, entitlementService);
    }

    @Test
    void budgetReportReusesTheEntitlementSummaryAndComputesUtilisation() {
        when(entitlementService.getEntitlementSummary(2L, 2026))
                .thenReturn(Optional.of(summary(2L, "Ben Tan", 2026, 2000, 500, 1500)));
        when(entitlementService.findOccupyingApplications(2L, 2026)).thenReturn(List.of(
                application(ben, "External course", CourseCategory.EXTERNAL, "APPROVED", 500)));
        when(entitlementService.getEntitlementSummary(3L, 2026))
                .thenReturn(Optional.of(summary(3L, "Cathy Lim", 2026, 1000, 1000, 0)));
        when(entitlementService.findOccupyingApplications(3L, 2026)).thenReturn(List.of());

        BudgetReport report = service.findBudgetReport(manager, null, 2026);

        BudgetReportRow benRow = report.getRows().get(0);
        assertEquals(2000, benRow.getTrainingBudget());
        assertEquals(500, benRow.getClaimedFees());
        assertEquals(1500, benRow.getRemainingBudget());
        assertEquals(25.0, benRow.getUtilisationPercent(), 0.0001);
        assertEquals(2, report.getEmployeeCount());
        assertEquals(2, report.getConfiguredCount());
        assertEquals(3000, report.getTotalTrainingBudget());
        assertEquals(1500, report.getTotalClaimedFees());
        assertEquals(50.0, report.getOverallUtilisationPercent(), 0.0001);
    }

    @Test
    void budgetReportMarksOnlyExternalCoursesAndCertificationsAsBudgetRelevant() {
        when(entitlementService.getEntitlementSummary(2L, 2026))
                .thenReturn(Optional.of(summary(2L, "Ben Tan", 2026, 2000, 500, 1500)));
        when(entitlementService.findOccupyingApplications(2L, 2026)).thenReturn(List.of(
                application(ben, "Internal course", CourseCategory.INTERNAL, "APPROVED",
                        LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2), 0),
                application(ben, "External course", CourseCategory.EXTERNAL, "APPROVED",
                        LocalDate.of(2026, 3, 3), LocalDate.of(2026, 3, 4), 500),
                application(ben, "Certification", CourseCategory.CERTIFICATION, "COMPLETED",
                        LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 6), 250)));
        when(entitlementService.getEntitlementSummary(3L, 2026)).thenReturn(Optional.empty());

        BudgetReport report = service.findBudgetReport(manager, null, 2026);

        var details = report.getRows().get(0).getDetails();
        assertEquals(3, details.size());
        assertFalse(details.get(0).isCountsTowardsBudget());
        assertTrue(details.get(1).isCountsTowardsBudget());
        assertTrue(details.get(2).isCountsTowardsBudget());
        assertEquals("External course", details.get(1).getCategoryLabel());
    }

    @Test
    void budgetReportHandlesAMissingEntitlementWithoutFailing() {
        when(entitlementService.getEntitlementSummary(2L, 2026)).thenReturn(Optional.empty());
        when(entitlementService.getEntitlementSummary(3L, 2026)).thenReturn(Optional.empty());

        BudgetReport report = service.findBudgetReport(manager, null, 2026);

        assertEquals(2, report.getEmployeeCount());
        assertEquals(0, report.getConfiguredCount());
        assertEquals(0, report.getTotalTrainingBudget());
        assertEquals(0, report.getOverallUtilisationPercent());
        for (BudgetReportRow row : report.getRows()) {
            assertFalse(row.isHasEntitlement());
            assertEquals(0, row.getTrainingBudget());
            assertEquals(0, row.getClaimedFees());
            assertEquals(0, row.getUtilisationPercent());
            assertTrue(row.getDetails().isEmpty());
        }
        verify(entitlementService, never()).findOccupyingApplications(2L, 2026);
    }

    @Test
    void budgetReportAvoidsDivisionByZeroWhenTheBudgetIsZero() {
        when(entitlementService.getEntitlementSummary(2L, 2026))
                .thenReturn(Optional.of(summary(2L, "Ben Tan", 2026, 0, 250, -250)));
        when(entitlementService.findOccupyingApplications(2L, 2026)).thenReturn(List.of());
        when(entitlementService.getEntitlementSummary(3L, 2026)).thenReturn(Optional.empty());

        BudgetReport report = service.findBudgetReport(manager, null, 2026);

        assertEquals(0, report.getRows().get(0).getUtilisationPercent());
        assertEquals(0, report.getOverallUtilisationPercent());
    }

    @Test
    void budgetReportForOneEmployeeOnlyReturnsThatEmployee() {
        when(entitlementService.getEntitlementSummary(3L, 2026))
                .thenReturn(Optional.of(summary(3L, "Cathy Lim", 2026, 1000, 100, 900)));
        when(entitlementService.findOccupyingApplications(3L, 2026)).thenReturn(List.of());

        BudgetReport report = service.findBudgetReport(manager, cathy.getId(), 2026);

        assertEquals(1, report.getEmployeeCount());
        assertEquals("Cathy Lim", report.getRows().get(0).getEmployeeName());
        verify(entitlementService, never()).getEntitlementSummary(2L, 2026);
    }

    private List<String> titles(AttendanceReport report) {
        List<String> titles = new ArrayList<>();
        for (AttendanceReportRow row : report.getRows()) {
            titles.add(row.getCourseTitle());
        }
        return titles;
    }

    private Employee employee(Long id, String name, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        employee.setUsername(name.toLowerCase().replace(' ', '.'));
        employee.setDesignation(EmployeeDesignation.PROFESSIONAL);
        employee.setRole(role);
        return employee;
    }

    private Employee subordinate(Long id, String name, Employee supervisor) {
        Employee employee = employee(id, name, EmployeeRole.REGULAR_STAFF);
        employee.setSupervisor(supervisor);
        return employee;
    }

    private CourseApplication application(
            Employee employee, String title, String category, String status, double fee) {
        return application(employee, title, category, status,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12), fee);
    }

    private CourseApplication application(
            Employee employee, String title, String category, String status,
            LocalDate from, LocalDate to) {
        return application(employee, title, category, status, from, to, 100);
    }

    private CourseApplication application(
            Employee employee, String title, String category, String status,
            LocalDate from, LocalDate to, double fee) {
        CourseApplication application = new CourseApplication();
        application.setEmployee(employee);
        application.setCourseTitle(title);
        application.setCategory(category);
        application.setStatus(status);
        application.setFromDate(from);
        application.setToDate(to);
        application.setFee(fee);
        application.setTrainingDays(2);
        return application;
    }

    private EntitlementSummary summary(
            Long employeeId, String name, int year, double budget, double occupied, double remaining) {
        EntitlementSummary summary = new EntitlementSummary();
        summary.setEmployeeId(employeeId);
        summary.setEmployeeName(name);
        summary.setEntitlementYear(year);
        summary.setTrainingBudget(budget);
        summary.setOccupiedBudget(occupied);
        summary.setRemainingBudget(remaining);
        summary.setTrainingDaysLimit(10);
        summary.setOccupiedDays(2);
        summary.setRemainingDays(8);
        return summary;
    }
}
