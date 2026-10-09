package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:course-catalogue;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
@Transactional
class CourseCatalogueIntegrationTests {
    @Autowired CourseRepository courses;
    @Autowired CourseService courseService;
    @Autowired CommonCourseRepository common;
    @Autowired TrainingProviderRepository providers;
    @Autowired CourseApplicationRepository applications;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void duplicateValidationUsesTitleCategoryAndProviderAndAllowsEditingItself() {
        TrainingProvider provider = providers.save(new TrainingProvider("Identity provider"));
        Course course = courses.saveAndFlush(new Course("Java", "EXTERNAL", provider, 200));
        com.group5.cats.dto.CourseForm form = new com.group5.cats.dto.CourseForm();
        form.setTitle(" java ");
        form.setIntroduction("Learn Java through exercises.");
        form.setCategory("EXTERNAL");
        form.setProviderId(provider.getId());
        form.setFee(300.0);

        assertNotNull(courseService.createCourse(form));
        assertNull(courseService.updateCourse(course.getId(), form));

        Course other = courses.saveAndFlush(new Course("Other", "EXTERNAL", provider, 100));
        assertNotNull(courseService.updateCourse(other.getId(), form));
        assertEquals("Other", other.getTitle());

        TrainingProvider second = providers.save(new TrainingProvider("Second provider"));
        form.setProviderId(second.getId());
        assertNull(courseService.createCourse(form));
        form.setProviderId(provider.getId());
        form.setCategory("CERTIFICATION");
        assertNull(courseService.createCourse(form));
    }

    @Test
    void catalogueStatusFollowsAddingAndRemovingTheEntry() {
        TrainingProvider provider = providers.save(new TrainingProvider("Status provider"));
        Course course = courses.save(new Course("Status course", "EXTERNAL", provider, 200));
        assertFalse(courseService.findCommonCourseIds().contains(course.getId()));
        CommonCourse entry = common.saveAndFlush(new CommonCourse(course));
        assertTrue(courseService.findCommonCourseIds().contains(course.getId()));
        common.delete(entry);
        common.flush();
        assertFalse(courseService.findCommonCourseIds().contains(course.getId()));
        assertTrue(courses.existsById(course.getId()));
    }

    @Test
    void relationshipsPersistAndRemovingCommonEntryKeepsCourseAndSnapshot() {
        TrainingProvider provider = providers.save(new TrainingProvider("Catalogue provider"));
        Course course = courses.save(new Course("Java", "EXTERNAL", provider, 200));
        CommonCourse entry = common.saveAndFlush(new CommonCourse(course));
        CourseApplication application = new CourseApplication();
        application.setCourse(course);
        application.setCourseTitle(course.getTitle());
        application.setProvider(provider.getName());
        application.setCategory(course.getCategory());
        application.setFee(150);
        applications.saveAndFlush(application);

        Long courseId = course.getId();
        Long applicationId = application.getId();
        assertTrue(common.existsByCourse_Id(courseId));
        assertTrue(courses.existsByProvider_Id(provider.getId()));
        assertTrue(applications.existsByCourse_Id(courseId));
        assertEquals("Java", common.findAllByOrderByCourse_TitleAsc().get(0).getCourse().getTitle());

        common.delete(entry);
        course.setTitle("Java revised");
        provider.setName("Provider renamed");
        entityManager.flush();
        entityManager.clear();
        assertTrue(courses.findById(courseId).isPresent());
        assertFalse(common.existsByCourse_Id(courseId));
        CourseApplication saved = applications.findById(applicationId).orElseThrow();
        assertEquals("Java", saved.getCourseTitle());
        assertEquals("Catalogue provider", saved.getProvider());
        assertEquals(150, saved.getFee());
        assertEquals("Java revised", saved.getCourse().getTitle());
    }

    @Test
    void databaseRejectsDuplicateCommonCourseReferences() {
        TrainingProvider provider = providers.save(new TrainingProvider("Unique provider"));
        Course course = courses.save(new Course("Unique course", "INTERNAL", provider, 0));
        common.saveAndFlush(new CommonCourse(course));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> common.saveAndFlush(new CommonCourse(course)));
    }

    @Test
    void legacyMembershipMigrationPreservesExistingIdsAndDetails() throws Exception {
        // A separate temporary database simulates the old CommonCourse table.
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:legacy-courses;MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE courses (id BIGINT PRIMARY KEY, title VARCHAR(255))");
            statement.execute("INSERT INTO courses VALUES (42, 'Legacy Java')");
            ScriptUtils.executeSqlScript(connection,
                    new FileSystemResource(Path.of("database/migrate-common-course-catalogue.sql")));
            try (var result = statement.executeQuery(
                    "SELECT c.id, c.title, cc.course_id FROM courses c JOIN common_courses cc ON cc.course_id = c.id")) {
                assertTrue(result.next());
                assertEquals(42, result.getLong(1));
                assertEquals("Legacy Java", result.getString(2));
                assertEquals(42, result.getLong(3));
                assertFalse(result.next());
            }
        }
    }
    @Test
    void introductionDurationAndStartDatesSurviveReload() {
        TrainingProvider provider = providers.save(new TrainingProvider("Schedule provider"));
        Course course = new Course("Scheduled course", "EXTERNAL", provider, 100);
        course.setIntroduction("Practice with real examples.");
        course.setDurationDays(2);
        course.getStartDates().add(java.time.LocalDate.of(2027, 1, 4));
        course.getStartDates().add(java.time.LocalDate.of(2027, 1, 18));
        courses.saveAndFlush(course);
        Long id = course.getId();
        entityManager.clear();
        Course reloaded = courses.findById(id).orElseThrow();
        assertEquals(2, reloaded.getDurationDays());
        assertEquals("Practice with real examples.", reloaded.getIntroduction());
        assertEquals(2, reloaded.getStartDates().size());
        assertEquals(java.time.LocalDate.of(2027, 1, 4), reloaded.getStartDates().get(0));
    }

}
