package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.dto.CommonCourseForm;
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
class CourseCatalogueIntegrationTests {
    @Autowired CommonCourseRepository courses;
    @Autowired CommonCourseService courseService;
    @Autowired CategoryRepository categories;
    @Autowired CategoryService categoryService;
    @Autowired TrainingProviderRepository providers;
    @Autowired CourseApplicationRepository applications;
    @Autowired EmployeeRepository employees;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void duplicateIdentityIsCheckedAndOwnUpdateAllowed() {
        TrainingProvider provider = providers.save(new TrainingProvider("Identity provider"));
        Category category = categories.findByNameIgnoreCase("EXTERNAL").orElseThrow();
        CommonCourse course = courses.saveAndFlush(new CommonCourse("Java", category, provider, 200));
        CommonCourseForm form = new CommonCourseForm();
        form.setTitle(" java "); form.setIntroduction("Learn Java.");
        form.setCategoryId(category.getId()); form.setProviderId(provider.getId()); form.setFee(300.0);
        assertNotNull(courseService.createCommonCourse(form));
        assertNull(courseService.updateCommonCourse(course.getId(), form));
        CommonCourse other = courses.saveAndFlush(new CommonCourse("Other", category, provider, 100));
        assertNotNull(courseService.updateCommonCourse(other.getId(), form));
        assertEquals("Other", other.getTitle());
    }

    @Test
    void catalogueDeletionPreservesApplicationAndBlocksCategoryDeletion() {
        assertNull(categoryService.createCategory("WORKSHOP", "Workshop", true));
        Category category = categories.findByNameIgnoreCase("WORKSHOP").orElseThrow();
        TrainingProvider provider = providers.save(new TrainingProvider("Catalogue provider"));
        CommonCourse course = courses.saveAndFlush(new CommonCourse("Java", category, provider, 200));
        course.setIntroduction("Java introduction");
        CourseApplication application = new CourseApplication();
        application.setEmployee(employees.findByUsername("emp1").orElseThrow());
        application.setCourseTitle("Java"); application.setProvider(provider.getName());
        application.setCategory(category); application.setFee(150);
        applications.saveAndFlush(application);
        Long id = application.getId();
        assertNotNull(categoryService.deleteCategory(category.getId()));
        assertNull(courseService.deleteCommonCourse(course.getId()));
        provider.setName("Provider renamed");
        entityManager.flush(); entityManager.clear();
        CourseApplication saved = applications.findById(id).orElseThrow();
        assertEquals("Java", saved.getCourseTitle());
        assertEquals("Catalogue provider", saved.getProvider());
        assertEquals(150, saved.getFee());
        assertEquals("WORKSHOP", saved.getCategoryName());
        assertNotNull(categoryService.deleteCategory(category.getId()));
    }

    @Test
    void originalCategoriesExistWithFixedSettings() {
        for (String name : List.of("INTERNAL", "EXTERNAL", "CERTIFICATION")) {
            Category category = categories.findByNameIgnoreCase(name).orElseThrow();
            assertEquals(name.equals("INTERNAL"), category.isHalfDayAllowed());
            assertNotNull(categoryService.deleteCategory(category.getId()));
        }
    }

    @Test
    void categoryRelationshipAndReferenceFeeSurviveReload() {
        Category category = categories.findByNameIgnoreCase("INTERNAL").orElseThrow();
        TrainingProvider provider = providers.save(new TrainingProvider("Fee provider"));
        CommonCourse course = new CommonCourse("Recorded internal cost", category, provider, 125);
        course.setIntroduction("Recorded cost, excluded from entitlement budget.");
        courses.saveAndFlush(course);
        Long id = course.getId(); entityManager.clear();
        CommonCourse saved = courses.findById(id).orElseThrow();
        assertEquals(125, saved.getFee());
        assertEquals("INTERNAL", saved.getCategory().getName());
        assertTrue(saved.getCategory().isHalfDayAllowed());
        assertTrue(saved.getIntroduction().contains("excluded"));
    }
}
