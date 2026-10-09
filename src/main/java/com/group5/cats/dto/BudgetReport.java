package com.group5.cats.dto;

import java.util.List;

/** Course fee claim and budget utilisation report for a reporting year. */
public class BudgetReport {

    private Integer year;
    private List<BudgetReportRow> rows = List.of();

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<BudgetReportRow> getRows() {
        return rows;
    }

    public void setRows(List<BudgetReportRow> rows) {
        this.rows = rows == null ? List.of() : rows;
    }

    public int getEmployeeCount() {
        return rows.size();
    }

    /** Employees with an entitlement row for the year; the rest cannot report a budget. */
    public int getConfiguredCount() {
        int configured = 0;
        for (BudgetReportRow row : rows) {
            if (row.isHasEntitlement()) {
                configured++;
            }
        }
        return configured;
    }

    public double getTotalTrainingBudget() {
        double total = 0.0;
        for (BudgetReportRow row : rows) {
            if (row.isHasEntitlement()) {
                total += row.getTrainingBudget();
            }
        }
        return total;
    }

    /** Course fee claims committed or approved in the year, using the entitlement rule. */
    public double getTotalClaimedFees() {
        double total = 0.0;
        for (BudgetReportRow row : rows) {
            if (row.isHasEntitlement()) {
                total += row.getClaimedFees();
            }
        }
        return total;
    }

    public double getTotalRemainingBudget() {
        double total = 0.0;
        for (BudgetReportRow row : rows) {
            if (row.isHasEntitlement()) {
                total += row.getRemainingBudget();
            }
        }
        return total;
    }

    public double getOverallUtilisationPercent() {
        double budget = getTotalTrainingBudget();
        if (budget <= 0) {
            return 0.0;
        }
        return getTotalClaimedFees() / budget * 100.0;
    }
}
