package com.group5.cats.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "courses", uniqueConstraints = @UniqueConstraint(
        name = "uk_course_identity", columnNames = {"title", "category_id", "provider_id"}))
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private TrainingProvider provider;
    @Column(nullable = false)
    private double fee;
    @Column(length = 4000)
    private String introduction;
    @Column(nullable = false)
    private boolean active = true;

    public Course() { }

    public Course(String title, Category category, TrainingProvider provider, double fee) {
        this.title = title;
        this.category = category;
        this.provider = provider;
        this.fee = fee;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public TrainingProvider getProvider() { return provider; }
    public void setProvider(TrainingProvider provider) { this.provider = provider; }
    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
