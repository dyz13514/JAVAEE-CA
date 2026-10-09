package com.group5.cats.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// All courses maintained by administrators.
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private TrainingProvider provider;

    @Column(nullable = false)
    private double fee;

    @Column(length = 4000)
    private String introduction;

    // Nullable for old catalogue rows that have not been scheduled yet.
    private Integer durationDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_start_dates", joinColumns = @JoinColumn(name = "course_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "start_date"}))
    @Column(name = "start_date", nullable = false)
    @OrderBy
    private List<LocalDate> startDates = new ArrayList<>();

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
    public List<LocalDate> getStartDates() {
        return startDates;
    }
    public void setStartDates(List<LocalDate> startDates) {
        this.startDates = startDates;
    }

    public Course() {
    }

    public Course(
            String title,
            String category,
            TrainingProvider provider,
            double fee) {

        this.title = title;
        this.category = category;
        this.provider = provider;
        this.fee = fee;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TrainingProvider getProvider() {
        return provider;
    }

    public void setProvider(TrainingProvider provider) {
        this.provider = provider;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }
}