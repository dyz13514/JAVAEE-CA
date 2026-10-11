package com.group5.cats.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "course_applications")
public class CourseApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    @Transient
    private Long courseId;
    private String categoryNameSnapshot;

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; this.courseId = null; }
    public Long getCourseId() {
        if (courseId != null) {
            return courseId;
        }
        if (course == null) {
            return null;
        }
        return course.getId();
    }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public void setCategoryNameSnapshot(String name) { this.categoryNameSnapshot = name; }

    private String courseTitle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 接收表单中的类别 ID，由 Service 查询对应类别。
    @Transient
    private Long categoryId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private TrainingProvider provider;

    // 接收表单中的机构 ID，由 Service 查询对应机构。
    @Transient
    private Long providerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private double fee;

    private String justification;

    private String dissemination;

    private String status = "APPLIED";

    private String managerComment;

    private String experienceComments;

    private double trainingDays;

    private Boolean halfDay;

    public CourseApplication() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        this.categoryId = null;
    }

    public Long getCategoryId() {
        if (categoryId != null) {
            return categoryId;
        }

        if (category == null) {
            return null;
        }

        return category.getId();
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        if (categoryNameSnapshot != null) return categoryNameSnapshot;
        if (category == null) {
            return null;
        }

        return category.getName();
    }

    public TrainingProvider getProvider() {
        return provider;
    }

    public void setProvider(TrainingProvider provider) {
        this.provider = provider;
        this.providerId = null;
    }

    public Long getProviderId() {
        if (providerId != null) {
            return providerId;
        }

        if (provider == null) {
            return null;
        }

        return provider.getId();
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        if (provider == null) {
            return null;
        }

        return provider.getName();
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

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getDissemination() {
        return dissemination;
    }

    public void setDissemination(String dissemination) {
        this.dissemination = dissemination;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public String getExperienceComments() {
        return experienceComments;
    }

    public void setExperienceComments(String experienceComments) {
        this.experienceComments = experienceComments;
    }

    public double getTrainingDays() {
        return trainingDays;
    }

    public void setTrainingDays(double trainingDays) {
        this.trainingDays = trainingDays;
    }

    public Boolean getHalfDay() {
        return halfDay;
    }

    public void setHalfDay(Boolean halfDay) {
        this.halfDay = halfDay;
    }
}
