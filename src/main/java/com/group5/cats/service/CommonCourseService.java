package com.group5.cats.service;

import java.util.List;

import com.group5.cats.model.CommonCourse;

public interface CommonCourseService {

    List<CommonCourse> findAllCommonCourses();

    List<CommonCourse> findCommonCourses(String keyword, Long categoryId, Long providerId);

    CommonCourse findCommonCourseById(Long id);

    String addCommonCourse(Long courseId);

    String deleteCommonCourse(Long id);


}
