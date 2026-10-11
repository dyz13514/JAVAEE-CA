package com.group5.cats;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.Category;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.CourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;
import com.group5.cats.service.CourseService;

@Component
@Order(1)
public class CourseTestDataLoader implements CommandLineRunner {
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final TrainingProviderRepository providerRepository;
    private final CourseService courseService;

    public CourseTestDataLoader(CourseRepository courseRepository,
            CategoryRepository categoryRepository,
            TrainingProviderRepository providerRepository, CourseService courseService) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.providerRepository = providerRepository;
        this.courseService = courseService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 按课程名称、类别和培训机构补齐测试课程，已有课程保持不变。
        TrainingProvider iss = findOrCreateProvider("ISS");
        TrainingProvider aws = findOrCreateProvider("AWS");
        TrainingProvider academy = findOrCreateProvider("Demo Skills Academy");
        for (String title : List.of("Java", "Python", "SQL",
                "Cloud Architecture", "Cybersecurity", "Project Management")) {
            createSample(title, "INTERNAL", iss, 0);
            TrainingProvider externalProvider = academy;
            if ("Java".equals(title) || "Cloud Architecture".equals(title)) {
                externalProvider = aws;
            }
            createSample(title, "EXTERNAL", externalProvider, 200);
            createSample(title, "CERTIFICATION", academy, 300);
        }
    }

    private void createSample(String title, String categoryName,
            TrainingProvider provider, double fee) {
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
        if (category == null) {
            throw new IllegalStateException("Default category not found: " + categoryName);
        }
        if (courseRepository.findByTitleIgnoreCaseAndCategory_IdAndProvider_Id(
                title, category.getId(), provider.getId()).isPresent()) {
            return;
        }
        String error = courseService.createCourse(title, category.getId(), provider.getId(), fee,
                "A practical introduction to " + title + ". Learn through guided exercises.");
        if (error != null) {
            throw new IllegalStateException(error);
        }
    }

    private TrainingProvider findOrCreateProvider(String name) {
        TrainingProvider provider = providerRepository.findByNameIgnoreCase(name).orElse(null);
        if (provider == null) {
            provider = providerRepository.save(new TrainingProvider(name));
        }
        return provider;
    }
}
