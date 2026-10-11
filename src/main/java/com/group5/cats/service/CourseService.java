package com.group5.cats.service;

import java.util.List;
import java.util.Optional;
import com.group5.cats.dto.CourseOption;
import com.group5.cats.model.Course;

public interface CourseService {
    List<CourseOption> findAvailableCourses();
    List<Course> findCourses(String keyword, Long categoryId, Long providerId);
    Optional<Course> findCourseById(Long id);

    String createCourse(String title, Long categoryId, Long providerId,
            Double fee, String introduction);

    String deleteCourse(Long id);
}
