package com.group5.cats.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.model.Category;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;

@Service
public class CommonCourseServiceImpl implements CommonCourseService {

    private final CommonCourseRepository commonCourseRepository;
    private final CategoryRepository categoryRepository;
    private final TrainingProviderRepository trainingProviderRepository;

    public CommonCourseServiceImpl(
            CommonCourseRepository commonCourseRepository,
            CategoryRepository categoryRepository,
            TrainingProviderRepository trainingProviderRepository) {

        this.commonCourseRepository = commonCourseRepository;
        this.categoryRepository = categoryRepository;
        this.trainingProviderRepository = trainingProviderRepository;
    }

    @Override
    public List<CommonCourse> findAllCommonCourses() {
        return commonCourseRepository.findAllByOrderByTitleAsc();
    }

    @Override
    public List<CommonCourse> findCommonCourses(String keyword, Long categoryId, Long providerId) {
        String search = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<CommonCourse> result = new ArrayList<>();
        for (CommonCourse course : findAllCommonCourses()) {
            if (categoryId != null && !categoryId.equals(course.getCategory().getId())) continue;
            if (providerId != null && !providerId.equals(course.getProvider().getId())) continue;
            String text = course.getTitle() + " " + course.getCategory().getName()
                    + " " + course.getProvider().getName() + " " + course.getIntroduction();
            if (text.toLowerCase(Locale.ROOT).contains(search)) result.add(course);
        }
        return result;
    }

    @Override
    public CommonCourse findCommonCourseById(Long id) {
        return commonCourseRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public String createCommonCourse(CommonCourseForm form) {

        String error = validateForm(form);
        if (error != null) {
            return error;
        }

        Category category = categoryRepository
                .findById(form.getCategoryId()).orElse(null);

        if (category == null) {
            return "Selected category does not exist.";
        }

        TrainingProvider provider = trainingProviderRepository
                .findById(form.getProviderId()).orElse(null);

        if (provider == null) {
            return "Selected training provider does not exist.";
        }


        String title = form.getTitle().trim();

        if (commonCourseRepository
                .existsByTitleIgnoreCaseAndCategory_IdAndProvider_Id(
                        title, category.getId(), provider.getId())) {
            return "This course already exists in the catalogue.";
        }

        CommonCourse course = new CommonCourse(
                title, category, provider, form.getFee());

        course.setIntroduction(form.getIntroduction().trim());
        commonCourseRepository.save(course);

        return null;
    }

    @Override
    @Transactional
    public String updateCommonCourse(Long id, CommonCourseForm form) {

        if (id == null || id <= 0) {
            return "Course ID must be positive.";
        }

        CommonCourse course =
                commonCourseRepository.findById(id).orElse(null);

        if (course == null) {
            return "Commonly attended course not found.";
        }

        String error = validateForm(form);
        if (error != null) {
            return error;
        }

        Category category = categoryRepository
                .findById(form.getCategoryId()).orElse(null);

        if (category == null) {
            return "Selected category does not exist.";
        }

        TrainingProvider provider = trainingProviderRepository
                .findById(form.getProviderId()).orElse(null);

        if (provider == null) {
            return "Selected training provider does not exist.";
        }


        String title = form.getTitle().trim();

        if (commonCourseRepository
                .existsByTitleIgnoreCaseAndCategory_IdAndProvider_IdAndIdNot(
                        title, category.getId(), provider.getId(), id)) {
            return "This course already exists in the catalogue.";
        }

        course.setTitle(title);
        course.setCategory(category);
        course.setProvider(provider);
        course.setFee(form.getFee());
        course.setIntroduction(form.getIntroduction().trim());

        commonCourseRepository.save(course);

        return null;
    }

    @Override
    @Transactional
    public String deleteCommonCourse(Long id) {

        if (id == null || id <= 0) {
            return "Course ID must be positive.";
        }

        CommonCourse course =
                commonCourseRepository.findById(id).orElse(null);

        if (course == null) {
            return "Commonly attended course not found.";
        }

        commonCourseRepository.delete(course);

        return null;
    }

    private String validateForm(CommonCourseForm form) {

        if (form == null) {
            return "Course information is required.";
        }

        if (form.getTitle() == null || form.getTitle().isBlank()) {
            return "Course title is required.";
        }

        if (form.getTitle().trim().length() > 255) {
            return "Course title must not exceed 255 characters.";
        }

        if (form.getCategoryId() == null || form.getCategoryId() <= 0) {
            return "Please select a valid category.";
        }

        if (form.getProviderId() == null || form.getProviderId() <= 0) {
            return "Please select a valid training provider.";
        }

        if (form.getFee() == null) {
            return "Course fee is required.";
        }

        if (!Double.isFinite(form.getFee()) || form.getFee() < 0) {
            return "Course fee must be a valid number greater than or equal to zero.";
        }

        if (form.getIntroduction() == null
                || form.getIntroduction().isBlank()
                || form.getIntroduction().trim().length() > 4000) {
            return "Please enter a course introduction of 1 to 4000 characters.";
        }

        return null;
    }
}
