package com.group5.cats.dto;

import java.time.LocalDate;

/** One confirmed course attendance line in the manager attendance report. */
public class AttendanceReportRow {

    private String employeeName;
    private String courseTitle;
    private String category;
    private LocalDate fromDate;
    private LocalDate toDate;
    private double trainingDays;
    private String status;
    private double fee;

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

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

    public double getTrainingDays() {
        return trainingDays;
    }

    public void setTrainingDays(double trainingDays) {
        this.trainingDays = trainingDays;
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

    public boolean isBudgetRelevant() {
        return CourseCategory.isBudgetRelevant(category);
    }
}
