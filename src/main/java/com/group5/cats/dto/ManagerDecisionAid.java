package com.group5.cats.dto;

import java.util.List;

import com.group5.cats.model.CourseApplication;

public class ManagerDecisionAid {

    private final boolean entitlementPresent;
    private final double usedDays;
    private final double limitDays;
    private final double usedBudget;
    private final double budgetLimit;
    private final List<CourseApplication> overlappingApproved;

    public ManagerDecisionAid(boolean entitlementPresent, double usedDays, double limitDays,
            double usedBudget, double budgetLimit, List<CourseApplication> overlappingApproved) {
        this.entitlementPresent = entitlementPresent;
        this.usedDays = usedDays;
        this.limitDays = limitDays;
        this.usedBudget = usedBudget;
        this.budgetLimit = budgetLimit;
        this.overlappingApproved = overlappingApproved == null ? List.of() : overlappingApproved;
    }

    public boolean isEntitlementPresent() {
        return entitlementPresent;
    }

    public double getUsedDays() {
        return usedDays;
    }

    public double getLimitDays() {
        return limitDays;
    }

    public double getUsedBudget() {
        return usedBudget;
    }

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public List<CourseApplication> getOverlappingApproved() {
        return overlappingApproved;
    }
}
