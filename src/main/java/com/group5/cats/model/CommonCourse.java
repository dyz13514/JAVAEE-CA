package com.group5.cats.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** A catalogue entry refers to the same course used by applications. */
@Entity
@Table(name = "common_courses")
public class CommonCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private Course course = new Course();

    public CommonCourse() { }
    public CommonCourse(Course course) { this.course = course; }
    public CommonCourse(String title, Category category, TrainingProvider provider, double fee) {
        this(new Course(title, category, provider, fee));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getTitle() { return course.getTitle(); }
    public void setTitle(String title) { course.setTitle(title); }
    public Category getCategory() { return course.getCategory(); }
    public void setCategory(Category category) { course.setCategory(category); }
    public TrainingProvider getProvider() { return course.getProvider(); }
    public void setProvider(TrainingProvider provider) { course.setProvider(provider); }
    public double getFee() { return course.getFee(); }
    public void setFee(double fee) { course.setFee(fee); }
    public String getIntroduction() { return course.getIntroduction(); }
    public void setIntroduction(String introduction) { course.setIntroduction(introduction); }
}
