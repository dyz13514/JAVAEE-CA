package com.group5.cats.model;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Transient;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity 
@Table(name = "course_applications")
public class CourseApplication {
 @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Employee employee;

    // Nullable for existing applications and manually entered courses.
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Transient
    private Long courseId;

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Long getCourseId() {
        if (courseId != null) return courseId;
        return course == null ? null : course.getId();
    }

    public void setCourseId(Long courseId) { this.courseId = courseId; }

    // Snapshot fields: catalogue updates must not rewrite application history.
    private String courseTitle;
    private String category;      // INTERNAL / EXTERNAL / CERTIFICATION

    private String provider;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

