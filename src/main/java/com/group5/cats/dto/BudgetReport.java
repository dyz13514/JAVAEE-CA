package com.group5.cats.dto;

import java.util.List;

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
