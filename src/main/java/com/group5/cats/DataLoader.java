package com.group5.cats;
import java.util.List;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CommonCourseRepository;
import com.group5.cats.repository.CategoryRepository;
import com.group5.cats.repository.TrainingProviderRepository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;

@Component 
@Order(1)
public class DataLoader implements CommandLineRunner {
	
private final EmployeeRepository employeeRepository;
private final CategoryRepository categoryRepository;
private final CommonCourseRepository commonCourseRepository;
private final TrainingProviderRepository providerRepository;


public DataLoader(EmployeeRepository employeeRepository,
		CategoryRepository categoryRepository,
        CommonCourseRepository commonCourseRepository,
        TrainingProviderRepository providerRepository) {
        this.employeeRepository = employeeRepository;
        this.categoryRepository = categoryRepository;
        this.commonCourseRepository = commonCourseRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    @Transactional
     public void run(String... args) throws Exception {
        boolean freshDemoDatabase = employeeRepository.count() == 0;
        if (freshDemoDatabase) {
           Employee manager = employeeRepository.save(
                new Employee("manager1", "manager123", "Alice Wong",
                        null, EmployeeDesignation.PROFESSIONAL, EmployeeRole.MANAGER));

            employeeRepository.saveAll(List.of(
                new Employee("admin", "admin123", "System Admin",
                        null, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.ADMIN),
                new Employee("emp1", "emp123", "Ben Tan",
                        manager, EmployeeDesignation.PROFESSIONAL, EmployeeRole.REGULAR_STAFF),
                new Employee("emp2", "emp123", "Cathy Lim",
                        manager, EmployeeDesignation.ADMINISTRATIVE, EmployeeRole.REGULAR_STAFF)
            ));
        }
        // Demo records only on the first initialization; do not undo later admin deletions.
        if (freshDemoDatabase && commonCourseRepository.count() == 0) {
            TrainingProvider internal = findOrCreateProvider("CATS Internal Training");
            TrainingProvider academy = findOrCreateProvider("Demo Skills Academy");
            TrainingProvider certification = findOrCreateProvider("Demo Certification Centre");
            List<CommonCourse> samples = List.of(
                sample("Java and Spring Boot Fundamentals", "EXTERNAL", academy, 650),
                sample("SQL and Database Design", "EXTERNAL", academy, 420),
                sample("Cloud Architecture Essentials", "EXTERNAL", academy, 780),
                sample("Workplace Communication", "INTERNAL", internal, 0),
                sample("Information Security Awareness", "INTERNAL", internal, 0),
                sample("Project Management Certification", "CERTIFICATION", certification, 1200)
            );
            commonCourseRepository.saveAll(samples);
        }
    }

    private CommonCourse sample(String title, String categoryName, TrainingProvider provider, double fee) {
        CommonCourse course = new CommonCourse(title,
                categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow(), provider, fee);
        course.setIntroduction("A practical introduction to " + title + ". Learn through guided exercises.");
        return course;
    }

    private TrainingProvider findOrCreateProvider(String name) {
        TrainingProvider provider = providerRepository.findByNameIgnoreCase(name).orElse(null);
        if (provider == null) {
            provider = providerRepository.save(new TrainingProvider(name));
        }
        return provider;
    }
}
