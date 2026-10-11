package com.group5.cats.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.dto.CourseOption;
import com.group5.cats.model.Category;
import com.group5.cats.model.Course;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.CourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;

@Service
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final TrainingProviderRepository providerRepository;
    private final CommonCourseRepository commonCourseRepository;
    private final CourseApplicationRepository applicationRepository;

    public CourseServiceImpl(CourseRepository courseRepository,
            CategoryRepository categoryRepository,
            TrainingProviderRepository providerRepository,
            CommonCourseRepository commonCourseRepository,
            CourseApplicationRepository applicationRepository) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.providerRepository = providerRepository;
        this.commonCourseRepository = commonCourseRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public List<CourseOption> findAvailableCourses() {
        List<CourseOption> options = new ArrayList<>();
        for (Course course : courseRepository.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc()) {
            options.add(new CourseOption(course));
        }
        return options;
    }

    @Override
    public List<Course> findCourses(String keyword, Long categoryId, Long providerId) {
        String search = "";
        if (keyword != null) {
            search = keyword.trim().toLowerCase(Locale.ROOT);
        }
        List<Course> results = new ArrayList<>();
        for (Course course : courseRepository.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc()) {
            if (categoryId != null && !categoryId.equals(course.getCategory().getId())) {
                continue;
            }
            if (providerId != null && !providerId.equals(course.getProvider().getId())) {
                continue;
            }
            String text = course.getTitle() + " " + course.getCategory().getName() + " "
                    + course.getProvider().getName() + " " + course.getIntroduction();
            if (text.toLowerCase(Locale.ROOT).contains(search)) {
                results.add(course);
            }
        }
        return results;
    }

    @Override
    public Optional<Course> findCourseById(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return courseRepository.findById(id).filter(Course::isActive);
    }

    @Override
    @Transactional
    public String createCourse(String title, Long categoryId, Long providerId,
            Double fee, String introduction) {
        if (title == null || title.isBlank()) {
            return "Course title is required.";
        }
        String courseTitle = title.trim();
        if (courseTitle.length() > 255) {
            return "Course title must not exceed 255 characters.";
        }
        if (categoryId == null || categoryId <= 0) {
            return "Please select a valid category.";
        }
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return "Selected category no longer exists.";
        }
        if (providerId == null || providerId <= 0) {
            return "Please select a valid training provider.";
        }
        TrainingProvider provider = providerRepository.findById(providerId).orElse(null);
        if (provider == null) {
            return "Selected training provider no longer exists.";
        }
        if (fee == null || !Double.isFinite(fee) || fee < 0) {
            return "Course fee must be a valid number greater than or equal to zero.";
        }
        if ("INTERNAL".equals(category.getName()) && fee != 0) {
            return "Internal training must have a zero course fee.";
        }
        String courseIntroduction = "";
        if (introduction != null) {
            courseIntroduction = introduction.trim();
        }
        if (courseIntroduction.length() > 4000) {
            return "Course introduction must not exceed 4000 characters.";
        }
        if (courseRepository.findByTitleIgnoreCaseAndCategory_IdAndProvider_Id(
                courseTitle, categoryId, providerId).isPresent()) {
            return "A course with this title, category and training provider already exists.";
        }

        Course course = new Course(courseTitle, category, provider, fee);
        course.setIntroduction(courseIntroduction);
        courseRepository.save(course);
        return null;
    }

    @Override
    @Transactional
    public String deleteCourse(Long id) {
        if (id == null || id <= 0) {
            return "Course ID must be positive.";
        }
        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) {
            return "Course not found.";
        }
        // 任何申请引用都保留课程，避免破坏历史记录。
        if (applicationRepository.existsByCourse_Id(id)) {
            return "This course is used by applications and cannot be deleted.";
        }
        if (commonCourseRepository.existsByCourse_Id(id)) {
            return "Remove this course from the common catalogue before deleting it.";
        }
        courseRepository.delete(course);
        return null;
    }
}
