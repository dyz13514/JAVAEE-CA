package com.group5.cats.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.group5.cats.dto.CourseOption;
import com.group5.cats.model.Course;
import com.group5.cats.repository.CourseRepository;

@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public List<CourseOption> findAvailableCourses() {
        return courseRepository.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc()
                .stream().map(CourseOption::new).toList();
    }

    @Override
    public List<Course> findCourses(String keyword, Long categoryId, Long providerId) {
        String search = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return courseRepository.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc().stream()
                .filter(course -> categoryId == null || categoryId.equals(course.getCategory().getId()))
                .filter(course -> providerId == null || providerId.equals(course.getProvider().getId()))
                .filter(course -> (course.getTitle() + " " + course.getCategory().getName() + " "
                        + course.getProvider().getName() + " " + course.getIntroduction()).toLowerCase(Locale.ROOT).contains(search))
                .toList();
    }

    @Override
    public Optional<Course> findCourseById(Long id) {
        return id == null || id <= 0 ? Optional.empty() : courseRepository.findById(id).filter(Course::isActive);
    }
}
