package com.group5.cats.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;

@Service
public class CommonCourseServiceImpl implements CommonCourseService {

    private final CommonCourseRepository commonCourseRepository;
    private final TrainingProviderRepository trainingProviderRepository;

    public CommonCourseServiceImpl(
            CommonCourseRepository commonCourseRepository,
            TrainingProviderRepository trainingProviderRepository) {

        this.commonCourseRepository = commonCourseRepository;
        this.trainingProviderRepository = trainingProviderRepository;
    }

    @Override
    public List<CommonCourse> findAllCommonCourses() {
        return commonCourseRepository.findAllByOrderByTitleAsc();
    }

    @Override
    public CommonCourse findCommonCourseById(Long id) {
        return commonCourseRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public String createCommonCourse(CommonCourseForm commonCourseForm) {

        String error = validateCommonCourseForm(commonCourseForm);

        if (error != null) {
            return error;
        }

        TrainingProvider provider = trainingProviderRepository.findById(
                commonCourseForm.getProviderId()
        ).orElse(null);

        if (provider == null) {
            return "Selected training provider does not exist.";
        }

        CommonCourse commonCourse = new CommonCourse();

        commonCourse.setTitle(commonCourseForm.getTitle().trim());
        commonCourse.setCategory(commonCourseForm.getCategory());
        commonCourse.setProvider(provider);
        commonCourse.setFee(commonCourseForm.getFee());

        commonCourseRepository.save(commonCourse);

        return null;
    }

    @Override
    @Transactional
    public String updateCommonCourse(
            Long id,
            CommonCourseForm commonCourseForm) {

        CommonCourse commonCourse =
                commonCourseRepository.findById(id).orElse(null);

        if (commonCourse == null) {
            return "Commonly attended course not found.";
        }

        String error = validateCommonCourseForm(commonCourseForm);

        if (error != null) {
            return error;
        }

        TrainingProvider provider = trainingProviderRepository.findById(
                commonCourseForm.getProviderId()
        ).orElse(null);

        if (provider == null) {
            return "Selected training provider does not exist.";
        }

        commonCourse.setTitle(commonCourseForm.getTitle().trim());
        commonCourse.setCategory(commonCourseForm.getCategory());
        commonCourse.setProvider(provider);
        commonCourse.setFee(commonCourseForm.getFee());

        commonCourseRepository.save(commonCourse);

        return null;
    }

    @Override
    @Transactional
    public String deleteCommonCourse(Long id) {

        CommonCourse commonCourse =
                commonCourseRepository.findById(id).orElse(null);

        if (commonCourse == null) {
            return "Commonly attended course not found.";
        }

        commonCourseRepository.delete(commonCourse);

        return null;
    }

    private String validateCommonCourseForm(
            CommonCourseForm commonCourseForm) {

        if (commonCourseForm.getTitle() == null
                || commonCourseForm.getTitle().isBlank()) {
            return "Course title is required.";
        }

        if (commonCourseForm.getTitle().trim().length() > 255) {
            return "Course title must not exceed 255 characters.";
        }

        String category = commonCourseForm.getCategory();

        if (!"INTERNAL".equals(category)
                && !"EXTERNAL".equals(category)
                && !"CERTIFICATION".equals(category)) {
            return "Please select a valid course category.";
        }

        if (commonCourseForm.getProviderId() == null) {
            return "Training provider is required.";
        }

        if (commonCourseForm.getFee() == null) {
            return "Course fee is required.";
        }

        double fee = commonCourseForm.getFee();

        if (!Double.isFinite(fee) || fee < 0) {
            return "Course fee must be a valid number greater than or equal to zero.";
        }

        if ("INTERNAL".equals(category) && fee != 0) {
            return "Internal courses must have a fee of zero.";
        }

        return null;
    }
}