package com.group5.cats.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import com.group5.cats.model.Employee;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.CourseFeeClaim;

public class TrainingLedger {

    private Employee employee;

    private Integer year;

    private boolean hasEntitlement;

    private double trainingBudget;

    private double occupiedBudget;

    private double remainingBudget;

    private BigDecimal approvedCourseFees = BigDecimal.ZERO;

    private BigDecimal pendingClaimAmount = BigDecimal.ZERO;

    private BigDecimal approvedClaimAmount = BigDecimal.ZERO;

    private BigDecimal reimbursedAmount = BigDecimal.ZERO;

    private List<CourseApplication> applications = new ArrayList<>();

    private List<CourseFeeClaim> claims = new ArrayList<>();

    public TrainingLedger() {
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
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

    public double getOccupiedBudget() {
        return occupiedBudget;
    }

    public void setOccupiedBudget(double occupiedBudget) {
        this.occupiedBudget = occupiedBudget;
    }

    public double getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(double remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public BigDecimal getApprovedCourseFees() {
        return approvedCourseFees;
    }

    public void setApprovedCourseFees(BigDecimal approvedCourseFees) {
        this.approvedCourseFees = approvedCourseFees;
    }

    public BigDecimal getPendingClaimAmount() {
        return pendingClaimAmount;
    }

    public void setPendingClaimAmount(BigDecimal pendingClaimAmount) {
        this.pendingClaimAmount = pendingClaimAmount;
    }

    public BigDecimal getApprovedClaimAmount() {
        return approvedClaimAmount;
    }

    public void setApprovedClaimAmount(BigDecimal approvedClaimAmount) {
        this.approvedClaimAmount = approvedClaimAmount;
    }

    public BigDecimal getReimbursedAmount() {
        return reimbursedAmount;
    }

    public void setReimbursedAmount(BigDecimal reimbursedAmount) {
        this.reimbursedAmount = reimbursedAmount;
    }

    public List<CourseApplication> getApplications() {
        return applications;
    }

    public void setApplications(List<CourseApplication> applications) {
        this.applications = applications;
    }

    public List<CourseFeeClaim> getClaims() {
        return claims;
    }

    public void setClaims(List<CourseFeeClaim> claims) {
        this.claims = claims;
    }
}
