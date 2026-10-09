package com.group5.cats.service;

import java.util.Comparator;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import com.group5.cats.model.CommonCourse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.dto.CourseForm;
import com.group5.cats.model.Course;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CourseRepository;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.TrainingProviderRepository;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TrainingProviderRepository trainingProviderRepository;

    private final CommonCourseRepository commonCourseRepository;
    private final CourseApplicationRepository applicationRepository;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            TrainingProviderRepository trainingProviderRepository,
            CommonCourseRepository commonCourseRepository,
            CourseApplicationRepository applicationRepository) {

        this.courseRepository = courseRepository;
        this.trainingProviderRepository = trainingProviderRepository;
        this.commonCourseRepository = commonCourseRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public List<Course> findAllCourses() {
        return courseRepository.findAllByOrderByTitleAsc();
    }

    @Override
    public List<Course> findCourses(String keyword, String category, Long providerId, boolean commonOnly) {
        String search = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Long> commonCourseIds = new ArrayList<>();
        if (commonOnly) {
            commonCourseIds = findCommonCourseIds();
        }
        List<Course> matches = new ArrayList<>();
        for (Course course : courseRepository.findAllByOrderByTitleAsc()) {
            if (!search.isEmpty()
                    && !course.getTitle().toLowerCase(Locale.ROOT).contains(search)
                    && !course.getProvider().getName().toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }
            if (category != null && !category.isBlank() && !category.equals(course.getCategory())) {
                continue;
            }
            if (providerId != null && !providerId.equals(course.getProvider().getId())) {
                continue;
            }
            if (commonOnly && !commonCourseIds.contains(course.getId())) {
                continue;
            }
            matches.add(course);
        }
        return matches;
    }

    @Override
    public List<Long> findCommonCourseIds() {
        List<Long> courseIds = new ArrayList<>();
        for (CommonCourse entry : commonCourseRepository.findAll()) {
            courseIds.add(entry.getCourse().getId());
        }
        return courseIds;
    }

    @Override
    public Course findCourseById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public String createCourse(CourseForm courseForm) {

        String error = validateCourseForm(courseForm);

        if (error != null) {
            return error;
        }

        TrainingProvider provider = trainingProviderRepository.findById(
                courseForm.getProviderId()
        ).orElse(null);

        if (provider == null) {
            return "Selected training provider does not exist.";
        }

        if (courseRepository.existsByTitleIgnoreCaseAndCategoryAndProvider_Id(
                courseForm.getTitle().trim(), courseForm.getCategory(), provider.getId())) {
            return "A course with this title and category already exists for this provider.";
        }

        Course course = new Course();

        course.setTitle(courseForm.getTitle().trim());
        course.setCategory(courseForm.getCategory());
        course.setProvider(provider);
        course.setFee(courseForm.getFee());
        course.setIntroduction(courseForm.getIntroduction().trim());
        course.setDurationDays(courseForm.getDurationDays());
        course.getStartDates().clear();
        course.getStartDates().addAll(parseStartDates(courseForm.getStartDates()));

        courseRepository.save(course);

        return null;
    }

    @Override
    @Transactional
    public String updateCourse(
            Long id,
            CourseForm courseForm) {

        Course course =
                courseRepository.findById(id).orElse(null);

        if (course == null) {
            return "Course not found.";
        }

        String error = validateCourseForm(courseForm);

        if (error != null) {
            return error;
        }

        TrainingProvider provider = trainingProviderRepository.findById(
                courseForm.getProviderId()
        ).orElse(null);

        if (provider == null) {
            return "Selected training provider does not exist.";
        }

        if (courseRepository.existsByTitleIgnoreCaseAndCategoryAndProvider_IdAndIdNot(
                courseForm.getTitle().trim(), courseForm.getCategory(), provider.getId(), id)) {
            return "A course with this title and category already exists for this provider.";
        }

        course.setTitle(courseForm.getTitle().trim());
        course.setCategory(courseForm.getCategory());
        course.setProvider(provider);
        course.setFee(courseForm.getFee());
        course.setIntroduction(courseForm.getIntroduction().trim());
        course.setDurationDays(courseForm.getDurationDays());
        course.getStartDates().clear();
        course.getStartDates().addAll(parseStartDates(courseForm.getStartDates()));

        courseRepository.save(course);

        return null;
    }

    @Override
    @Transactional
    public String deleteCourse(Long id) {

        Course course =
                courseRepository.findById(id).orElse(null);

        if (course == null) {
            return "Course not found.";
        }

        if (commonCourseRepository.existsByCourse_Id(id)) {
            return "Remove this course from the commonly attended catalogue first.";
        }
        if (applicationRepository.existsByCourse_Id(id)) {
            return "This course has applications and cannot be deleted.";
        }
        courseRepository.delete(course);

        return null;
    }

    private String validateCourseForm(
            CourseForm courseForm) {

        if (courseForm.getTitle() == null
                || courseForm.getTitle().isBlank()) {
            return "Course title is required.";
        }

        if (courseForm.getTitle().trim().length() > 255) {
            return "Course title must not exceed 255 characters.";
        }

        if (courseForm.getIntroduction() == null || courseForm.getIntroduction().isBlank()
                || courseForm.getIntroduction().length() > 4000) {
            return "Please enter a course introduction of 1 to 4000 characters.";
        }
        if (courseForm.getDurationDays() == null || courseForm.getDurationDays() < 1
                || courseForm.getDurationDays() > 30) {
            return "Duration must be between 1 and 30 training days.";
        }
        try {
            parseStartDates(courseForm.getStartDates());
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }

        String category = courseForm.getCategory();

        if (!"INTERNAL".equals(category)
                && !"EXTERNAL".equals(category)
                && !"CERTIFICATION".equals(category)) {
            return "Please select a valid course category.";
        }

        if (courseForm.getProviderId() == null) {
            return "Training provider is required.";
        }

        if (courseForm.getFee() == null) {
            return "Course fee is required.";
        }

        double fee = courseForm.getFee();

        if (!Double.isFinite(fee) || fee < 0) {
            return "Course fee must be a valid number greater than or equal to zero.";
        }

        if ("INTERNAL".equals(category) && fee != 0) {
            return "Internal courses must have a fee of zero.";
        }

        return null;
    }
    private List<LocalDate> parseStartDates(String value) {
        List<LocalDate> dates = new ArrayList<>();
        // An empty list means the course is visible but not open for applications.
        if (value == null || value.isBlank()) return dates;
        for (String text : value.trim().split("[,\\s]+")) {
            LocalDate date;
            try {
                date = LocalDate.parse(text);
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("Use YYYY-MM-DD for each start date.");
            }
            if (date.getYear() < 1 || date.getYear() > 9998
                    || date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                throw new IllegalArgumentException("Start dates must be weekdays within years 1 to 9998.");
            }
            if (dates.contains(date)) throw new IllegalArgumentException("Start dates must not be repeated.");
            dates.add(date);
        }
        dates.sort(Comparator.naturalOrder());
        return dates;
    }

}