package com.group5.cats.service;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.AttendanceReportRow;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.BudgetReportRow;
import com.group5.cats.dto.CourseFeeDetail;

/**
 * Renders the manager reports as CSV. The exported records are exactly the rows
 * of the report the manager is looking at, so both use the same filters and the
 * same attendance and entitlement rules.
 */
public final class ManagerReportCsv {

    private static final String[] ATTENDANCE_HEADERS = {
            "Employee Name", "Course Name", "Course Category", "Start Date",
            "End Date", "Training Days", "Status", "Course Fee (S$)"};

    private static final String[] BUDGET_HEADERS = {
            "Employee Name", "Reporting Year", "Annual Training Budget (S$)",
            "Course Name", "Course Category", "Start Date", "End Date", "Status",
            "Course Fee (S$)", "Counts Toward Budget", "Employee Fee Claims (S$)",
            "Employee Remaining Budget (S$)", "Employee Budget Utilisation (%)"};

    private ManagerReportCsv() {
    }

    public static String attendanceReport(AttendanceReport report) {
        StringBuilder csv = new StringBuilder(CsvSupport.UTF8_BOM);
        csv.append(CsvSupport.row(ATTENDANCE_HEADERS));
        for (AttendanceReportRow row : report.getRows()) {
            csv.append(CsvSupport.row(
                    row.getEmployeeName(),
                    row.getCourseTitle(),
                    row.getCategoryLabel(),
                    CsvSupport.formatDate(row.getFromDate()),
                    CsvSupport.formatDate(row.getToDate()),
                    CsvSupport.formatDecimal(row.getTrainingDays()),
                    row.getStatus(),
                    CsvSupport.formatMoney(row.getFee())));
        }
        return csv.toString();
    }

    /**
     * One row per contributing course fee claim. An employee with no claims still
     * gets a row so the annual budget position is visible; an employee without an
     * entitlement row exports blank budget columns.
     */
    public static String budgetReport(BudgetReport report) {
        StringBuilder csv = new StringBuilder(CsvSupport.UTF8_BOM);
        csv.append(CsvSupport.row(BUDGET_HEADERS));
        for (BudgetReportRow row : report.getRows()) {
            if (row.getDetails().isEmpty()) {
                csv.append(budgetRow(row, null));
                continue;
            }
            for (CourseFeeDetail detail : row.getDetails()) {
                csv.append(budgetRow(row, detail));
            }
        }
        return csv.toString();
    }

    private static String budgetRow(BudgetReportRow row, CourseFeeDetail detail) {
        boolean configured = row.isHasEntitlement();
        return CsvSupport.row(
                row.getEmployeeName(),
                row.getEntitlementYear() == null ? "" : row.getEntitlementYear().toString(),
                configured ? CsvSupport.formatMoney(row.getTrainingBudget()) : "",
                detail == null ? "" : detail.getCourseTitle(),
                detail == null ? "" : detail.getCategoryLabel(),
                detail == null ? "" : CsvSupport.formatDate(detail.getFromDate()),
                detail == null ? "" : CsvSupport.formatDate(detail.getToDate()),
                detail == null ? "" : detail.getStatus(),
                detail == null ? "" : CsvSupport.formatMoney(detail.getFee()),
                detail == null ? "" : (detail.isCountsTowardsBudget() ? "Yes" : "No"),
                configured ? CsvSupport.formatMoney(row.getClaimedFees()) : "",
                configured ? CsvSupport.formatMoney(row.getRemainingBudget()) : "",
                configured ? CsvSupport.formatDecimal(row.getUtilisationPercent()) : "");
    }
}
