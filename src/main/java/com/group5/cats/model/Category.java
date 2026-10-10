package com.group5.cats.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course_categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean halfDayAllowed;

    public Category() {
    }

    public Category(String name, String description,
            boolean halfDayAllowed) {
        this.name = name;
        this.description = description;
        this.halfDayAllowed = halfDayAllowed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHalfDayAllowed() {
        return halfDayAllowed;
    }

    public void setHalfDayAllowed(boolean halfDayAllowed) {
        this.halfDayAllowed = halfDayAllowed;
    }
}