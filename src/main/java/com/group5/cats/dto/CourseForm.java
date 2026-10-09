package com.group5.cats.dto;

public class CourseForm {

    private String title;
    private String category;
    private Long providerId;
    private Double fee;

    private String introduction;
    private Integer durationDays = 2;
    private String startDates;

    public String getIntroduction() {
        return introduction;
    }
    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }
    public Integer getDurationDays() {
        return durationDays;
    }
    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }
    public String getStartDates() {
        return startDates;
    }
    public void setStartDates(String startDates) {
        this.startDates = startDates;
    }

    public CourseForm() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public Double getFee() {
        return fee;
    }

    public void setFee(Double fee) {
        this.fee = fee;
    }
}