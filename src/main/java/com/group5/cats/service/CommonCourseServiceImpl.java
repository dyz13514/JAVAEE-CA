package com.group5.cats.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.Course;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.CourseRepository;

@Service
public class CommonCourseServiceImpl implements CommonCourseService {
    private final CommonCourseRepository commonCourseRepository;
    private final CourseRepository courseRepository;

    public CommonCourseServiceImpl(CommonCourseRepository commonCourseRepository,
            CourseRepository courseRepository) {
        this.commonCourseRepository = commonCourseRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public List<CommonCourse> findAllCommonCourses() {
        return commonCourseRepository.findAllByOrderByCourse_TitleAsc();
    }

    @Override
    public CommonCourse findCommonCourseById(Long id) {
        return commonCourseRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public String createCommonCourse(CommonCourseForm form) {
        if (form.getCourseId() == null || form.getCourseId() <= 0) {
            return "Please select a course.";
        }
        Course course = courseRepository.findById(form.getCourseId()).orElse(null);
        if (course == null) {
            return "Selected course does not exist.";
        }
        if (commonCourseRepository.existsByCourse_Id(course.getId())) {
            return "This course is already in the commonly attended catalogue.";
        }
        commonCourseRepository.save(new CommonCourse(course));
        return null;
    }

    @Override
    @Transactional
    public String updateCommonCourse(Long id, CommonCourseForm form) {
        CommonCourse entry = commonCourseRepository.findById(id).orElse(null);
        if (entry == null) {
            return "Commonly attended course not found.";
        }
        if (form.getCourseId() == null || form.getCourseId() <= 0) {
            return "Please select a course.";
        }
        Course course = courseRepository.findById(form.getCourseId()).orElse(null);
        if (course == null) {
            return "Selected course does not exist.";
        }
        if (commonCourseRepository.existsByCourse_IdAndIdNot(course.getId(), id)) {
            return "This course is already in the commonly attended catalogue.";
        }
        entry.setCourse(course);
        commonCourseRepository.save(entry);
        return null;
    }

    @Override
    @Transactional
    public String deleteCommonCourse(Long id) {
        CommonCourse entry = commonCourseRepository.findById(id).orElse(null);
        if (entry == null) {
            return "Commonly attended course not found.";
        }
        // No cascade: removing the catalogue entry never deletes the Course.
        commonCourseRepository.delete(entry);
        return null;
    }
}
