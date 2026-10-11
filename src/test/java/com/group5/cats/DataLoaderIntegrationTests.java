package com.group5.cats;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:course-catalogue;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "cats.mail.enabled=false"
})
@Transactional
class DataLoaderIntegrationTests {
    @Autowired CategoryDataLoader categoryLoader;
    @Autowired CourseTestDataLoader courseLoader;
    @Autowired DataLoader employeeLoader;
    @Autowired EntitlementDataLoader entitlementLoader;
    @Autowired CategoryRepository categories;
    @Autowired TrainingProviderRepository providers;
    @Autowired CourseRepository courses;
    @Autowired CommonCourseRepository commonCourses;
    @Autowired EmployeeRepository employees;
    @Autowired AnnualEntitlementRepository entitlements;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private void runAllLoaders() throws Exception {
        categoryLoader.run();
        courseLoader.run();
        employeeLoader.run();
        entitlementLoader.run();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void repeatedRunsKeepCredentialsRelationshipsAndCounts() throws Exception {
        runAllLoaders();
        runAllLoaders();
        assertEquals(3, categories.count());
        assertEquals(3, providers.count());
        assertEquals(18, courses.count());
        assertEquals(0, commonCourses.count());
        assertEquals(5, employees.count());
        assertEquals(1, entitlements.count());
        assertEquals("manager123", employees.findByUsername("manager1").orElseThrow().getPassword());
        assertEquals("admin123", employees.findByUsername("admin").orElseThrow().getPassword());
        for (String username : java.util.List.of("emp1", "emp2")) {
            Employee employee = employees.findByUsername(username).orElseThrow();
            assertEquals("emp123", employee.getPassword());
            assertEquals("manager1", employee.getSupervisor().getUsername());
        }
        assertEquals("entitlememt_test_password",
                employees.findByUsername("entitlememt_test").orElseThrow().getPassword());
        assertTrue(categories.findByNameIgnoreCase("INTERNAL").orElseThrow().isHalfDayAllowed());
        assertFalse(categories.findByNameIgnoreCase("EXTERNAL").orElseThrow().isHalfDayAllowed());
        assertFalse(categories.findByNameIgnoreCase("CERTIFICATION").orElseThrow().isHalfDayAllowed());
    }

    @Test
    void missingFixturesAreAddedEvenWithUnrelatedRecordsAlreadyPresent() throws Exception {
        courses.deleteAll();
        providers.deleteAll();
        categories.deleteAll();
        employees.delete(employees.findByUsername("emp2").orElseThrow());
        entityManager.flush();
        Category customCategory = categories.save(new Category("WORKSHOP", "Keep this category", true));
        TrainingProvider customProvider = providers.save(new TrainingProvider("Custom provider"));
        Course customCourse = courses.save(new Course("Custom course", customCategory, customProvider, 123));
        Employee customEmployee = employees.save(new Employee("custom", "custom123", "Custom employee",
                null, EmployeeDesignation.PROFESSIONAL, EmployeeRole.REGULAR_STAFF));
        Long courseId = customCourse.getId();
        Long employeeId = customEmployee.getId();

        runAllLoaders();
        runAllLoaders();
        assertEquals(4, categories.count());
        assertEquals(4, providers.count());
        assertEquals(19, courses.count());
        assertEquals(6, employees.count());
        assertEquals("Custom course", courses.findById(courseId).orElseThrow().getTitle());
        assertEquals(123, courses.findById(courseId).orElseThrow().getFee());
        assertEquals("custom123", employees.findById(employeeId).orElseThrow().getPassword());
        Employee restored = employees.findByUsername("emp2").orElseThrow();
        assertEquals("emp123", restored.getPassword());
        assertEquals("manager1", restored.getSupervisor().getUsername());
        assertEquals(0, commonCourses.count());
    }

    @Test
    void existingEditsArePreservedAndMissingCurrentYearEntitlementIsRestored() throws Exception {
        Category category = categories.findByNameIgnoreCase("EXTERNAL").orElseThrow();
        category.setDescription("Administrator's description");
        TrainingProvider provider = providers.findByNameIgnoreCase("AWS").orElseThrow();
        Course course = courses.findByTitleIgnoreCaseAndCategory_IdAndProvider_Id(
                "Java", category.getId(), provider.getId()).orElseThrow();
        Long courseId = course.getId();
        course.setFee(456);
        course.setIntroduction("Administrator's introduction");
        course.setActive(false);
        Employee staff = employees.findByUsername("emp1").orElseThrow();
        staff.setName("Updated staff name");
        staff.setPassword("Updated password");
        Employee testEmployee = employees.findByUsername("entitlememt_test").orElseThrow();
        Long employeeId = testEmployee.getId();
        entitlements.deleteByEmployeeId(employeeId);
        entityManager.flush();
        entitlements.save(new AnnualEntitlement(testEmployee, LocalDate.now().getYear() - 1, 12, 800));

        runAllLoaders();
        Course savedCourse = courses.findById(courseId).orElseThrow();
        assertEquals(456, savedCourse.getFee());
        assertEquals("Administrator's introduction", savedCourse.getIntroduction());
        assertFalse(savedCourse.isActive());
        assertEquals("Administrator's description",
                categories.findByNameIgnoreCase("EXTERNAL").orElseThrow().getDescription());
        assertEquals("Updated staff name", employees.findByUsername("emp1").orElseThrow().getName());
        assertEquals("Updated password", employees.findByUsername("emp1").orElseThrow().getPassword());
        AnnualEntitlement current = entitlements.findByEmployeeIdAndEntitlementYear(
                employeeId, LocalDate.now().getYear()).orElseThrow();
        assertEquals(50, current.getTrainingDaysLimit());
        assertEquals(20000, current.getTrainingBudget());
        current.setTrainingDaysLimit(15);
        current.setTrainingBudget(900);

        runAllLoaders();
        AnnualEntitlement retained = entitlements.findByEmployeeIdAndEntitlementYear(
                employeeId, LocalDate.now().getYear()).orElseThrow();
        assertEquals(15, retained.getTrainingDaysLimit());
        assertEquals(900, retained.getTrainingBudget());
        assertEquals(800, entitlements.findByEmployeeIdAndEntitlementYear(
                employeeId, LocalDate.now().getYear() - 1).orElseThrow().getTrainingBudget());
        assertEquals(2, entitlements.count());
        assertEquals(18, courses.count());
    }
}
