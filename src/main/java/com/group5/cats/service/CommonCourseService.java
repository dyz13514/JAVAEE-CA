package com.group5.cats.service;

import java.util.List;

import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.model.CommonCourse;

public interface CommonCourseService {

    List<CommonCourse> findAllCommonCourses();

    CommonCourse findCommonCourseById(Long id);

    String createCommonCourse(CommonCourseForm commonCourseForm);

    String updateCommonCourse(
            Long id,
            CommonCourseForm commonCourseForm);

    String deleteCommonCourse(Long id);
}