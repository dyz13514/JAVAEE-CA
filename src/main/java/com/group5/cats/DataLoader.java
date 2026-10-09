package com.group5.cats;
import java.util.List;
import com.group5.cats.model.Course;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.repository.CourseRepository;
import com.group5.cats.repository.TrainingProviderRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.EntitlementService;

@Component 
@Order(1)
public class DataLoader implements CommandLineRunner {
	
private final EmployeeRepository employeeRepository;
private final EntitlementService entitlementService;
private final CourseRepository courseRepository;
private final TrainingProviderRepository providerRepository;


public DataLoader(EmployeeRepository employeeRepository,
		EntitlementService entitlementService,
        CourseRepository courseRepository,
        TrainingProviderRepository providerRepository) {
        this.employeeRepository = employeeRepository;
        this.entitlementService = entitlementService;
        this.courseRepository = courseRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    @Transactional
     public void run(String... args) throws Exception {
        if (employeeRepository.count() == 0) {
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
        // Sample catalogue only: preserve existing courses and the admin's common selections.
        if (courseRepository.count() == 0) {
            TrainingProvider internal = findOrCreateProvider("CATS Internal Training");
            TrainingProvider academy = findOrCreateProvider("Demo Skills Academy");
            TrainingProvider certification = findOrCreateProvider("Demo Certification Centre");
            List<Course> samples = List.of(
                new Course("Java and Spring Boot Fundamentals", "EXTERNAL", academy, 650),
                new Course("SQL and Database Design", "EXTERNAL", academy, 420),
                new Course("Cloud Architecture Essentials", "EXTERNAL", academy, 780),
                new Course("Workplace Communication", "INTERNAL", internal, 0),
                new Course("Information Security Awareness", "INTERNAL", internal, 0),
                new Course("Project Management Certification", "CERTIFICATION", certification, 1200)
            );
            LocalDate firstStart = LocalDate.now().plusWeeks(2)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            for (Course course : samples) {
                course.setIntroduction("A practical introduction to " + course.getTitle()
                        + ". Learn the core concepts through guided exercises and workplace examples.");
                course.setDurationDays(2);
                course.getStartDates().add(firstStart);
                course.getStartDates().add(firstStart.plusWeeks(2));
            }
            courseRepository.saveAll(samples);
        }
        completeSampleSchedules();
    }

    private void completeSampleSchedules() {
        List<String> sampleTitles = List.of("Java and Spring Boot Fundamentals", "SQL and Database Design",
                "Cloud Architecture Essentials", "Workplace Communication", "Information Security Awareness",
                "Project Management Certification");
        List<String> sampleProviders = List.of("CATS Internal Training", "Demo Skills Academy", "Demo Certification Centre");
        LocalDate firstStart = LocalDate.now().plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        for (Course course : courseRepository.findAll()) {
            if (sampleTitles.contains(course.getTitle()) && sampleProviders.contains(course.getProvider().getName())
                    && course.getDurationDays() == null && course.getIntroduction() == null) {
                course.setIntroduction("A practical introduction to " + course.getTitle()
                        + ". Learn through guided exercises and workplace examples.");
                course.setDurationDays(2);
                course.getStartDates().add(firstStart);
                course.getStartDates().add(firstStart.plusWeeks(2));
                courseRepository.save(course);
            }
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
