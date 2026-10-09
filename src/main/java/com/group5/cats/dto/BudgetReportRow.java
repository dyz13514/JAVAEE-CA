package com.group5.cats.dto;

import java.util.List;

/**
 * Annual course fee claim position for one employee.
 *
 * <p>The budget figures come from the same entitlement calculation used by the
 * annual entitlement page and API, so the numbers agree everywhere. When an
 * employee has no entitlement row for the year the budget fields are zero and
 * {@link #isHasEntitlement()} is false instead of failing.
 */
public class BudgetReportRow {

    private Long employeeId;
    private String employeeName;
    private Integer entitlementYear;
    private boolean hasEntitlement;
    private double trainingBudget;
    private double claimedFees;
    private double remainingBudget;
    private double utilisationPercent;
    private List<CourseFeeDetail> details = List.of();

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Integer getEntitlementYear() {
        return entitlementYear;
    }

    public void setEntitlementYear(Integer entitlementYear) {
        this.entitlementYear = entitlementYear;
    }

    public boolean isHasEntitlement() {
        return hasEntitlement;
    }

    public void setHasEntitlement(boolean hasEntitlement) {
        this.hasEntitlement = hasEntitlement;
    }

    public double getTrainingBudget() {
        return trainingBudget;
    }

    public void setTrainingBudget(double trainingBudget) {
        this.trainingBudget = trainingBudget;
    }

    public double getClaimedFees() {
        return claimedFees;
    }

    public void setClaimedFees(double claimedFees) {
        this.claimedFees = claimedFees;
    }

    public double getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(double remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public double getUtilisationPercent() {
        return utilisationPercent;
    }

    public void setUtilisationPercent(double utilisationPercent) {
        this.utilisationPercent = utilisationPercent;
    }

    public List<CourseFeeDetail> getDetails() {
        return details;
    }

    public void setDetails(List<CourseFeeDetail> details) {
        this.details = details == null ? List.of() : details;
    }
}
