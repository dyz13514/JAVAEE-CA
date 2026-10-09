package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.AttendanceReportRow;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.BudgetReportRow;
import com.group5.cats.dto.CourseFeeDetail;

class ManagerReportCsvTests {

    private static final String ATTENDANCE_HEADER =
            "Employee Name,Course Name,Course Category,Start Date,End Date,Training Days,Status,Course Fee (S$)";

    @Test
    void attendanceCsvStartsWithTheBomAndTheExpectedHeaders() {
        AttendanceReport report = new AttendanceReport();
        report.setRows(List.of());

        String csv = ManagerReportCsv.attendanceReport(report);

        assertTrue(csv.startsWith(CsvSupport.UTF8_BOM), "export must declare UTF-8 for spreadsheets");
        assertEquals(CsvSupport.UTF8_BOM + ATTENDANCE_HEADER + "\r\n", csv);
    }

    @Test
    void attendanceCsvFormatsDatesCategoriesDaysAndFeesConsistently() {
        AttendanceReport report = new AttendanceReport();
        report.setRows(List.of(row("Ben Tan", "Cloud Architecture", "EXTERNAL",
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4), 3, "APPROVED", 1250)));

        String csv = ManagerReportCsv.attendanceReport(report);

        assertTrue(csv.contains("Ben Tan,Cloud Architecture,External course,2026-03-02,2026-03-04,3.00,APPROVED,1250.00"),
                csv);
    }

    @Test
    void attendanceCsvEscapesCommasQuotesAndLineBreaksInCourseNames() {
        AttendanceReportRow row = row("Cathy Lim", "Design, \"Advanced\"\nworkshop", "INTERNAL",
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 6), 0.5, "COMPLETED", 0);

        String csv = ManagerReportCsv.attendanceReport(reportWith(row));

        assertTrue(csv.contains("\"Design, \"\"Advanced\"\"\nworkshop\""), csv);
    }

    @Test
    void attendanceCsvNeutralisesSpreadsheetFormulas() {
        String csv = ManagerReportCsv.attendanceReport(reportWith(row("Ben Tan", "=cmd|'/C calc'!A0",
                "EXTERNAL", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2), 1, "APPROVED", 10)));

        assertTrue(csv.contains("'=cmd|'/C calc'!A0"), csv);
        assertFalse(csv.contains(",=cmd"), csv);
    }

    @Test
    void budgetCsvListsEveryFeeClaimAgainstTheAnnualPosition() {
        BudgetReport report = new BudgetReport();
        report.setYear(2026);
        BudgetReportRow employee = new BudgetReportRow();
        employee.setEmployeeName("Ben Tan");
        employee.setEntitlementYear(2026);
        employee.setHasEntitlement(true);
        employee.setTrainingBudget(20000);
        employee.setClaimedFees(1250);
        employee.setRemainingBudget(18750);
        employee.setUtilisationPercent(6.25);
        employee.setDetails(List.of(detail("Cloud Architecture", "EXTERNAL",
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4), "APPROVED", 1250, true)));
        report.setRows(List.of(employee));

        String csv = ManagerReportCsv.budgetReport(report);

        assertTrue(csv.contains("Employee Budget Utilisation (%)"), csv);
        assertTrue(csv.contains("Ben Tan,2026,20000.00,Cloud Architecture,External course,"
                + "2026-03-02,2026-03-04,APPROVED,1250.00,Yes,1250.00,18750.00,6.25"), csv);
    }

    @Test
    void budgetCsvKeepsEmployeesWithoutClaimsAndLeavesMissingBudgetsBlank() {
        BudgetReport report = new BudgetReport();
        report.setYear(2026);

        BudgetReportRow noClaims = new BudgetReportRow();
        noClaims.setEmployeeName("Cathy Lim");
        noClaims.setEntitlementYear(2026);
        noClaims.setHasEntitlement(true);
        noClaims.setTrainingBudget(20000);
        noClaims.setClaimedFees(0);
        noClaims.setRemainingBudget(20000);
        noClaims.setUtilisationPercent(0);

        BudgetReportRow noEntitlement = new BudgetReportRow();
        noEntitlement.setEmployeeName("New Starter");
        noEntitlement.setEntitlementYear(2026);

        report.setRows(List.of(noClaims, noEntitlement));

        String csv = ManagerReportCsv.budgetReport(report);

        assertTrue(csv.contains("Cathy Lim,2026,20000.00,,,,,,,,0.00,20000.00,0.00"), csv);
        assertTrue(csv.contains("New Starter,2026,,,,,,,,,,,"), csv);
    }

    private static AttendanceReport reportWith(AttendanceReportRow row) {
        AttendanceReport report = new AttendanceReport();
        report.setRows(List.of(row));
        return report;
    }

    private static AttendanceReportRow row(
            String employee, String course, String category, LocalDate from, LocalDate to,
            double days, String status, double fee) {
        AttendanceReportRow row = new AttendanceReportRow();
        row.setEmployeeName(employee);
        row.setCourseTitle(course);
        row.setCategory(category);
        row.setFromDate(from);
        row.setToDate(to);
        row.setTrainingDays(days);
        row.setStatus(status);
        row.setFee(fee);
        return row;
    }

    private static CourseFeeDetail detail(
            String course, String category, LocalDate from, LocalDate to,
            String status, double fee, boolean countsTowardsBudget) {
        CourseFeeDetail detail = new CourseFeeDetail();
        detail.setCourseTitle(course);
        detail.setCategory(category);
        detail.setFromDate(from);
        detail.setToDate(to);
        detail.setStatus(status);
        detail.setFee(fee);
        detail.setCountsTowardsBudget(countsTowardsBudget);
        return detail;
    }
}
