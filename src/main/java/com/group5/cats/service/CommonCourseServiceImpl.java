package com.group5.cats.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.Course;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.CourseRepository;

@Service
@Transactional(readOnly = true)
public class CommonCourseServiceImpl implements CommonCourseService {
    private final CommonCourseRepository commonCourseRepository;
    private final CourseRepository courseRepository;

    public CommonCourseServiceImpl(CommonCourseRepository commonCourseRepository, CourseRepository courseRepository) {
        this.commonCourseRepository = commonCourseRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public List<CommonCourse> findAllCommonCourses() {
        return commonCourseRepository.findAllByOrderByTitleAsc();
    }

    @Override
    public List<CommonCourse> findCommonCourses(String keyword, Long categoryId, Long providerId) {
        String search = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return findAllCommonCourses().stream()
                .filter(entry -> entry.getCourse().isActive())
                .filter(entry -> categoryId == null || categoryId.equals(entry.getCategory().getId()))
                .filter(entry -> providerId == null || providerId.equals(entry.getProvider().getId()))
                .filter(entry -> (entry.getTitle() + " " + entry.getCategory().getName() + " "
                        + entry.getProvider().getName() + " " + entry.getIntroduction()).toLowerCase(Locale.ROOT).contains(search))
                .toList();
    }

    @Override
    public CommonCourse findCommonCourseById(Long id) {
        return commonCourseRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public String addCommonCourse(Long courseId) {
        if (courseId == null || courseId <= 0) return "Please select a valid course.";
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || !course.isActive()) return "Selected course is no longer available.";
        if (commonCourseRepository.existsByCourse_Id(courseId)) return "This course is already in the common catalogue.";
        commonCourseRepository.save(new CommonCourse(course));
        return null;
    }

    @Override
    @Transactional
    public String deleteCommonCourse(Long id) {
        if (id == null || id <= 0) return "Catalogue entry ID must be positive.";
        CommonCourse entry = commonCourseRepository.findById(id).orElse(null);
        if (entry == null) return "Commonly attended course not found.";
        commonCourseRepository.delete(entry);
        return null;
    }
}
