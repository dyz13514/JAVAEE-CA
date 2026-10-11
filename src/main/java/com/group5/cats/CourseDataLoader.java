package com.group5.cats;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.Course;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.CourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;

@Component
@Order(2)
public class CourseDataLoader implements CommandLineRunner {
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final TrainingProviderRepository providerRepository;

    public CourseDataLoader(CourseRepository courseRepository,
            CategoryRepository categoryRepository, TrainingProviderRepository providerRepository) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Archived courses remain in this table, so restarts cannot recreate deleted entries.
        if (courseRepository.count() != 0) return;
        TrainingProvider iss = findOrCreateProvider("ISS");
        TrainingProvider aws = findOrCreateProvider("AWS");
        TrainingProvider academy = findOrCreateProvider("Demo Skills Academy");
        Random random = new Random(63);
        double[] fees = {200, 300, 500};
        List<Course> samples = new ArrayList<>();
        for (String title : List.of("Java", "Python", "SQL", "Cloud Architecture", "Cybersecurity", "Project Management")) {
            samples.add(sample(title, "INTERNAL", iss, 0));
            samples.add(sample(title, "EXTERNAL", title.equals("Java") || title.equals("Cloud Architecture") ? aws : academy,
                    fees[random.nextInt(fees.length)]));
            samples.add(sample(title, "CERTIFICATION", academy, fees[random.nextInt(fees.length)]));
        }
        courseRepository.saveAll(samples);
    }

    private Course sample(String title, String categoryName, TrainingProvider provider, double fee) {
        Course course = new Course(title, categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(), provider, fee);
        course.setIntroduction("A practical introduction to " + title + ". Learn through guided exercises.");
        return course;
    }

    private TrainingProvider findOrCreateProvider(String name) {
        return providerRepository.findByNameIgnoreCase(name).orElseGet(() -> providerRepository.save(new TrainingProvider(name)));
    }
}
