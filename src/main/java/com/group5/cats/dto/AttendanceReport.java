package com.group5.cats.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance report plus the summary figures shown above the table.
 */
public class AttendanceReport {

    private LocalDate fromDate;
    private LocalDate toDate;
    private String category;
    private List<AttendanceReportRow> rows = List.of();

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<AttendanceReportRow> getRows() {
        return rows;
    }

    public void setRows(List<AttendanceReportRow> rows) {
        this.rows = rows == null ? List.of() : rows;
    }

    public int getRecordCount() {
        return rows.size();
    }

    public double getTotalTrainingDays() {
        double total = 0.0;
        for (AttendanceReportRow row : rows) {
            total += row.getTrainingDays();
        }
        return total;
    }

    /** Every course fee in the report, including internal training (always zero). */
    public double getTotalFee() {
        double total = 0.0;
        for (AttendanceReportRow row : rows) {
            total += row.getFee();
        }
        return total;
    }

    /** Only external courses and certifications, matching the entitlement budget rule. */
    public double getBudgetRelevantFee() {
        double total = 0.0;
        for (AttendanceReportRow row : rows) {
            if (row.isBudgetRelevant()) {
                total += row.getFee();
            }
        }
        return total;
    }
}
