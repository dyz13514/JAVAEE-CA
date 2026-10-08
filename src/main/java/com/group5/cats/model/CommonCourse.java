package com.group5.cats.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// A commonly attended course maintained by administrators.
@Entity
@Table(name = "courses")
public class CommonCourse {

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

    public CommonCourse() {
    }

    public CommonCourse(
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