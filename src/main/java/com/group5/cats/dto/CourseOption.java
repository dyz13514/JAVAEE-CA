package com.group5.cats.dto;

import com.group5.cats.model.Course;

public record CourseOption(Long id, String title, Long categoryId, String categoryName,
        boolean halfDayAllowed, Long providerId, String providerName, double fee) {
    public CourseOption(Course course) {
        this(course.getId(), course.getTitle(), course.getCategory().getId(),
                course.getCategory().getName(), course.getCategory().isHalfDayAllowed(),
                course.getProvider().getId(), course.getProvider().getName(), course.getFee());
    }
}
