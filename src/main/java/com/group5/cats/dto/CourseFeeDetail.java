package com.group5.cats.dto;

import java.time.LocalDate;

/**
 * A single course application that contributes to an employee's annual budget claim.
 */
public class CourseFeeDetail {

    private String courseTitle;
    private String category;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String status;
    private double fee;
    private boolean countsTowardsBudget;

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryLabel() {
        return CourseCategory.label(category);
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public boolean isCountsTowardsBudget() {
        return countsTowardsBudget;
    }

    public void setCountsTowardsBudget(boolean countsTowardsBudget) {
        this.countsTowardsBudget = countsTowardsBudget;
    }
}
