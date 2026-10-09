package com.group5.cats.service;

import java.util.List;

import com.group5.cats.dto.CourseForm;
import com.group5.cats.model.Course;

public interface CourseService {

    List<Course> findAllCourses();

    List<Course> findCourses(String keyword, String category, Long providerId, boolean commonOnly);

    List<Long> findCommonCourseIds();

    Course findCourseById(Long id);

    String createCourse(CourseForm courseForm);

    String updateCourse(
            Long id,
            CourseForm courseForm);

    String deleteCourse(Long id);
}