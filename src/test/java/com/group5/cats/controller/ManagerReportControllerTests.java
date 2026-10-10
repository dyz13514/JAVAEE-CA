package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.AttendanceReportRow;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.BudgetReportRow;
import com.group5.cats.dto.CourseCategory;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CsvSupport;
import com.group5.cats.service.ManagerReportService;

class ManagerReportControllerTests {

    private ManagerReportService service;
    private MockMvc mvc;
    private Employee manager;

    @BeforeEach
    void setUp() {
        service = mock(ManagerReportService.class);
        var categories = mock(com.group5.cats.service.CategoryService.class);
        when(categories.findAllCategories()).thenReturn(List.of(
                com.group5.cats.CategoryFixtures.category("INTERNAL"), com.group5.cats.CategoryFixtures.category("EXTERNAL"),
                com.group5.cats.CategoryFixtures.category("CERTIFICATION")));
        mvc = MockMvcBuilders.standaloneSetup(new ManagerReportController(service, categories)).build();
        manager = employee(1L, "Alice Wong", EmployeeRole.MANAGER);
    }

    @Test
    void onlyAuthenticatedManagersReachTheReportPage() throws Exception {
        mvc.perform(get("/manager/reports")).andExpect(redirectedUrl("/employee/login"));
        for (EmployeeRole role : List.of(EmployeeRole.REGULAR_STAFF, EmployeeRole.ADMIN)) {
            mvc.perform(get("/manager/reports").session(session(employee(9L, "Other", role))))
                    .andExpect(redirectedUrl("/employee/home"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void managerSeesTheAttendanceReportWithFiltersAndTotals() throws Exception {
        AttendanceReport report = attendanceReport();
        when(service.findAttendanceReport(eq(manager), isNull(),
                eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31)), eq(CourseCategory.EXTERNAL)))
                .thenReturn(report);

        mvc.perform(get("/manager/reports").session(session(manager))
                        .param("report", "attendance").param("category", "EXTERNAL")
                        .param("startDate", "2026-03-01").param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-reports"))
                .andExpect(model().attribute("activeReport", "attendance"))
                .andExpect(model().attribute("attendanceReport", report))
                .andExpect(model().attribute("selectedCategory", "EXTERNAL"))
                .andExpect(model().attribute("startDate", LocalDate.of(2026, 3, 1)))
                .andExpect(model().attribute("endDate", LocalDate.of(2026, 3, 31)))
                .andExpect(model().attribute("categories", List.of("ALL", "INTERNAL", "EXTERNAL", "CERTIFICATION")));
    }

    @Test
    void managerSeesTheBudgetReportForTheSelectedYear() throws Exception {
        BudgetReport report = new BudgetReport();
        report.setYear(2025);
        when(service.findBudgetReport(manager, null, 2025)).thenReturn(report);

        mvc.perform(get("/manager/reports").session(session(manager))
                        .param("report", "budget").param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-reports"))
                .andExpect(model().attribute("activeReport", "budget"))
                .andExpect(model().attribute("budgetReport", report))
                .andExpect(model().attribute("selectedYear", 2025));
    }

    @Test
    void invalidFiltersShowAnErrorInsteadOfFailing() throws Exception {
        when(service.findAttendanceReport(manager, null,
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1), CourseCategory.ALL))
                .thenThrow(new IllegalArgumentException("The start date must not be after the end date."));

        mvc.perform(get("/manager/reports").session(session(manager))
                        .param("report", "attendance")
                        .param("startDate", "2026-03-31").param("endDate", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "The start date must not be after the end date."));

        mvc.perform(get("/manager/reports").session(session(manager))
                        .param("startDate", "31-03-2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Please enter dates in YYYY-MM-DD format."));
    }

    @Test
    void selectingAnotherManagersEmployeeIsReportedAsAnErrorAndCleared() throws Exception {
        when(service.findAttendanceReport(manager, 99L,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), CourseCategory.ALL))
                .thenThrow(new SecurityException("You are not allowed to report on this employee."));

        mvc.perform(get("/manager/reports").session(session(manager)).param("employeeId", "99")
                        .param("startDate", "2026-03-01").param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "You are not allowed to report on this employee."))
                .andExpect(model().attribute("selectedEmployeeId", (Object) null))
                .andExpect(model().attribute("attendanceReport", (Object) null));
    }

    @Test
    void malformedNumericParametersAreRejected() throws Exception {
        mvc.perform(get("/manager/reports").session(session(manager)).param("employeeId", "abc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/manager/reports").session(session(manager)).param("year", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attendanceExportUsesTheActiveFiltersAndReturnsADownload() throws Exception {
        when(service.findAttendanceReport(manager, 2L,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), CourseCategory.EXTERNAL))
                .thenReturn(attendanceReport());

        var response = mvc.perform(get("/manager/reports/attendance.csv").session(session(manager))
                        .param("employeeId", "2").param("category", "EXTERNAL")
                        .param("startDate", "2026-03-01").param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cats-attendance-report-2026-03-01-to-2026-03-31.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andReturn().getResponse();

        verify(service).findAttendanceReport(manager, 2L,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), CourseCategory.EXTERNAL);

        String csv = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.startsWith(CsvSupport.UTF8_BOM), csv);
        assertTrue(csv.contains("Employee Name,Course Name,Course Category,Start Date,End Date,"
                + "Training Days,Status,Course Fee (S$)"), csv);
        assertTrue(csv.contains("Ben Tan,\"Cloud, Advanced\",External course,2026-03-02,2026-03-04,3.00,APPROVED,1250.00"), csv);
    }

    @Test
    void attendanceExportHandlesAnEmptyResultWithHeadersOnly() throws Exception {
        AttendanceReport empty = new AttendanceReport();
        empty.setRows(List.of());
        when(service.findAttendanceReport(any(), any(), any(), any(), any())).thenReturn(empty);

        var response = mvc.perform(get("/manager/reports/attendance.csv").session(session(manager)))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        String csv = response.getContentAsString(StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n", -1);
        assertEquals(2, lines.length);
        assertEquals("", lines[1]);
        assertTrue(csv.endsWith("Course Fee (S$)\r\n"), csv);
    }

    @Test
    void budgetExportUsesTheReportingYearAndCarriesTheAnnualPosition() throws Exception {
        BudgetReport report = new BudgetReport();
        report.setYear(2026);
        BudgetReportRow row = new BudgetReportRow();
        row.setEmployeeName("Ben Tan");
        row.setEntitlementYear(2026);
        row.setHasEntitlement(true);
        row.setTrainingBudget(20000);
        row.setClaimedFees(1250);
        row.setRemainingBudget(18750);
        row.setUtilisationPercent(6.25);
        report.setRows(List.of(row));
        when(service.findBudgetReport(manager, null, 2026)).thenReturn(report);

        var response = mvc.perform(get("/manager/reports/budget.csv").session(session(manager)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"cats-budget-report-2026.csv\""))
                .andReturn().getResponse();

        String csv = response.getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.contains("Ben Tan,2026,20000.00,,,,,,,,1250.00,18750.00,6.25"), csv);
    }

    @Test
    void exportsRejectUnauthenticatedAndNonManagerCallers() throws Exception {
        mvc.perform(get("/manager/reports/attendance.csv")).andExpect(status().isUnauthorized());
        mvc.perform(get("/manager/reports/budget.csv")).andExpect(status().isUnauthorized());
        for (EmployeeRole role : List.of(EmployeeRole.REGULAR_STAFF, EmployeeRole.ADMIN)) {
            MockHttpSession session = session(employee(9L, "Other", role));
            mvc.perform(get("/manager/reports/attendance.csv").session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/manager/reports/budget.csv").session(session)).andExpect(status().isForbidden());
        }
        verify(service, org.mockito.Mockito.never()).findAttendanceReport(any(), any(), any(), any(), any());
    }

    @Test
    void exportsRejectEmployeesOutsideTheManagersTeam() throws Exception {
        when(service.findAttendanceReport(any(), any(), any(), any(), any()))
                .thenThrow(new SecurityException("You are not allowed to report on this employee."));
        when(service.findBudgetReport(any(), any(), any()))
                .thenThrow(new SecurityException("You are not allowed to report on this employee."));

        mvc.perform(get("/manager/reports/attendance.csv").session(session(manager)).param("employeeId", "99"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/manager/reports/budget.csv").session(session(manager)).param("employeeId", "99"))
                .andExpect(status().isForbidden());

        verify(service).findAttendanceReport(eq(manager), eq(99L), any(), any(), any());
        verify(service).findBudgetReport(eq(manager), eq(99L), any());
    }

    @Test
    void exportsRejectInvalidFilters() throws Exception {
        when(service.findBudgetReport(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("EntitlementYear must be between 1 and 9999."));
        when(service.findAttendanceReport(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("The start date must not be after the end date."));

        mvc.perform(get("/manager/reports/budget.csv").session(session(manager)).param("year", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/manager/reports/attendance.csv").session(session(manager))
                        .param("startDate", "2026-03-31").param("endDate", "2026-03-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/manager/reports/attendance.csv").session(session(manager))
                        .param("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest());
        // An unparsable date never reaches the service.
        mvc.perform(get("/manager/reports/attendance.csv").session(session(manager))
                        .param("startDate", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportTreatsAnEmptyEmployeeParameterAsAllDirectReports() throws Exception {
        when(service.findAttendanceReport(any(), isNull(), any(), any(), any()))
                .thenReturn(new AttendanceReport());

        mvc.perform(get("/manager/reports/attendance.csv").session(session(manager)).param("employeeId", ""))
                .andExpect(status().isOk());

        verify(service).findAttendanceReport(manager, null,
                LocalDate.of(LocalDate.now().getYear(), 1, 1),
                LocalDate.of(LocalDate.now().getYear(), 12, 31), CourseCategory.ALL);
    }

    private AttendanceReport attendanceReport() {
        AttendanceReportRow row = new AttendanceReportRow();
        row.setEmployeeName("Ben Tan");
        row.setCourseTitle("Cloud, Advanced");
        row.setCategory(CourseCategory.EXTERNAL);
        row.setFromDate(LocalDate.of(2026, 3, 2));
        row.setToDate(LocalDate.of(2026, 3, 4));
        row.setTrainingDays(3);
        row.setStatus("APPROVED");
        row.setFee(1250);

        AttendanceReport report = new AttendanceReport();
        report.setFromDate(LocalDate.of(2026, 3, 1));
        report.setToDate(LocalDate.of(2026, 3, 31));
        report.setCategory(CourseCategory.ALL);
        report.setRows(List.of(row));
        return report;
    }

    private MockHttpSession session(Employee employee) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", employee);
        return session;
    }

    private Employee employee(Long id, String name, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        employee.setUsername(name.toLowerCase().replace(' ', '.'));
        employee.setRole(role);
        return employee;
    }
}
