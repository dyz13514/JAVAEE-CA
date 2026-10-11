package com.group5.cats.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
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
class CourseCatalogueIntegrationTests {
    @Autowired CommonCourseRepository courses;
    @Autowired CourseRepository records;
    @Autowired CommonCourseService courseService;
    @Autowired CategoryRepository categories;
    @Autowired CategoryService categoryService;
    @Autowired TrainingProviderRepository providers;
    @Autowired TrainingProviderService providerService;
    @Autowired CourseApplicationRepository applications;
    @Autowired EmployeeRepository employees;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired CourseService courseAdminService;
    @Autowired CourseApplicationService applicationService;
    @Autowired EntitlementService entitlementService;
    @Autowired com.group5.cats.CourseTestDataLoader courseTestDataLoader;

    @Test
    void adminCanCreateACourseAndEmployeesApplyWithItsStoredDetails() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
        Employee admin = employees.findByUsername("admin").orElseThrow();
        Category category = categories.findByNameIgnoreCase("EXTERNAL").orElseThrow();
        TrainingProvider provider = providers.findByNameIgnoreCase("AWS").orElseThrow();
        mvc.perform(get("/admin/courses/new").sessionAttr("loggedInUser", admin))
                .andExpect(status().isOk()).andExpect(view().name("admin/course-form"))
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    assertTrue(html.contains("EXTERNAL"));
                    assertTrue(html.contains("AWS"));
                    assertTrue(html.contains("/admin/courses/save"));
                });
        mvc.perform(post("/admin/courses/save").sessionAttr("loggedInUser", admin)
                .param("title", " New Admin Course ").param("categoryId", category.getId().toString())
                .param("providerId", provider.getId().toString()).param("fee", "250")
                .param("introduction", "Practice course"))
                .andExpect(redirectedUrl("/admin/common-courses"))
                .andExpect(flash().attributeExists("successMessage"));
        Course created = records.findByTitleIgnoreCaseAndCategory_IdAndProvider_Id(
                "New Admin Course", category.getId(), provider.getId()).orElseThrow();
        assertTrue(courseAdminService.findAvailableCourses().stream()
                .anyMatch(option -> option.id().equals(created.getId())));
        mvc.perform(post("/admin/common-courses/" + created.getId() + "/add")
                .sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"))
                .andExpect(flash().attributeExists("successMessage"));
        entityManager.flush();
        assertTrue(courses.existsByCourse_Id(created.getId()));
        mvc.perform(get("/admin/common-courses").sessionAttr("loggedInUser", admin))
                .andExpect(status().isOk()).andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    assertTrue(html.contains("New Admin Course"));
                    assertTrue(html.contains("Remove from Common"));
                });
        assertNull(courseService.deleteCommonCourse(courses.findAll().get(0).getId()));
        entityManager.flush();
        Employee staff = employees.findByUsername("emp1").orElseThrow();
        java.time.LocalDate from = java.time.LocalDate.now().plusDays(1)
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        entitlementService.setEntitlement(staff.getId(), from.getYear(), 10, 1000);
        CourseApplication request = new CourseApplication();
        request.setCourseId(created.getId());
        request.setFromDate(from);
        request.setToDate(from.plusDays(1));
        request.setJustification("Use these skills at work.");
        request.setFee(1);
        request.setCourseTitle("Forged title");
        assertNull(applicationService.submitApplication(request, staff));
        entityManager.flush();
        assertEquals(created.getId(), request.getCourse().getId());
        assertEquals("New Admin Course", request.getCourseTitle());
        assertEquals(250, request.getFee());
        assertEquals(category.getId(), request.getCategoryId());
        assertEquals(provider.getId(), request.getProviderId());
        assertNotNull(courseAdminService.deleteCourse(created.getId()));
        request.setStatus("DELETED");
        entityManager.flush();
        assertNotNull(courseAdminService.deleteCourse(created.getId()));
        assertTrue(records.existsById(created.getId()));
    }

    @Test
    void invalidCreationPreservesFormAndRejectsBadReferencesFeesAndDuplicateIdentity() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
        Employee admin = employees.findByUsername("admin").orElseThrow();
        Category internal = categories.findByNameIgnoreCase("INTERNAL").orElseThrow();
        TrainingProvider provider = providers.findByNameIgnoreCase("ISS").orElseThrow();
        long before = records.count();
        mvc.perform(post("/admin/courses/save").sessionAttr("loggedInUser", admin)
                .param("title", "Internal test").param("categoryId", internal.getId().toString())
                .param("providerId", provider.getId().toString()).param("fee", "20")
                .param("introduction", "Retain this text"))
                .andExpect(status().isOk()).andExpect(view().name("admin/course-form"))
                .andExpect(model().attribute("title", "Internal test"))
                .andExpect(model().attribute("introduction", "Retain this text"))
                .andExpect(model().attributeExists("categories", "providers", "errorMessage"));
        for (Double fee : java.util.Arrays.asList(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNotNull(courseAdminService.createCourse("Invalid fee", internal.getId(), provider.getId(), fee, ""));
        }
        assertNotNull(courseAdminService.createCourse(" ", internal.getId(), provider.getId(), 0.0, ""));
        assertNotNull(courseAdminService.createCourse("Missing category", Long.MAX_VALUE, provider.getId(), 0.0, ""));
        assertNotNull(courseAdminService.createCourse("Missing provider", internal.getId(), Long.MAX_VALUE, 0.0, ""));
        assertNotNull(courseAdminService.createCourse("java", internal.getId(), provider.getId(), 0.0, ""));
        assertEquals(before, records.count());
    }

    @Test
    void courseMustLeaveCommonCatalogueBeforeDeletionAndRestartDoesNotUndoDeletion() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
        Employee admin = employees.findByUsername("admin").orElseThrow();
        Course course = records.findAll().get(0);
        assertNull(courseService.addCommonCourse(course.getId()));
        mvc.perform(post("/admin/courses/" + course.getId() + "/delete").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertTrue(records.existsById(course.getId()));
        assertNull(courseService.deleteCommonCourse(courses.findAll().get(0).getId()));
        entityManager.flush();
        mvc.perform(post("/admin/courses/" + course.getId() + "/delete").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"))
                .andExpect(flash().attributeExists("successMessage"));
        entityManager.flush();
        assertFalse(records.existsById(course.getId()));
        records.deleteAll();
        entityManager.flush();
        courseTestDataLoader.run();
        assertEquals(0, records.count());
        assertEquals("emp123", employees.findByUsername("emp1").orElseThrow().getPassword());
    }

    @Test
    void realPagesAndMembershipRequestsRespectRolesAndNeverChangeTheCourse() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).build();
        Employee admin = employees.findByUsername("admin").orElseThrow();
        Course fixed = records.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc().get(0);
        Long courseId = fixed.getId();
        double fee = fixed.getFee();
        String title = fixed.getTitle();
        long count = records.count();
        String addUrl = "/admin/common-courses/" + courseId + "/add";
        mvc.perform(post(addUrl).sessionAttr("loggedInUser", admin)
                .param("fee", "1").param("title", "Forged course").param("categoryId", "99"))
                .andExpect(redirectedUrl("/admin/common-courses"));
        mvc.perform(post(addUrl).sessionAttr("loggedInUser", admin))
                .andExpect(flash().attributeExists("errorMessage"));
        assertEquals(1, courses.count());
        Long entryId = courses.findAll().get(0).getId();
        for (String username : List.of("emp1", "manager1")) {
            Employee user = employees.findByUsername(username).orElseThrow();
            String html = mvc.perform(get("/employee/courses").sessionAttr("loggedInUser", user))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertTrue(html.contains("Commonly Attended Course Catalogue"));
            assertTrue(html.contains("All Course"));
            assertFalse(html.contains("Add to Common"));
            assertFalse(html.contains("Remove from Common"));
            mvc.perform(post(addUrl).sessionAttr("loggedInUser", user)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/common-courses/" + entryId + "/delete").sessionAttr("loggedInUser", user))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/admin/common-courses/" + entryId + "/delete").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"));
        entityManager.flush(); entityManager.clear();
        assertEquals(0, courses.count());
        assertEquals(count, records.count());
        Course saved = records.findById(courseId).orElseThrow();
        assertTrue(saved.isActive());
        assertEquals(title, saved.getTitle());
        assertEquals(fee, saved.getFee());
        Employee staff = employees.findByUsername("emp1").orElseThrow();
        mvc.perform(get("/employee/courses/details/" + courseId).sessionAttr("loggedInUser", staff))
                .andExpect(status().isOk());
        mvc.perform(get("/employee/apply").param("courseId", courseId.toString()).sessionAttr("loggedInUser", staff))
                .andExpect(status().isOk()).andExpect(model().attribute("courseLocked", true));
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", staff)
                .param("lockedCourseId", courseId.toString()).param("courseId", "999")
                .param("courseTitle", "Forged").param("fee", "1")
                .param("fromDate", "2027-01-04").param("toDate", "2027-01-05"))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attribute("courseLocked", true))
                .andExpect(result -> {
                    CourseApplication data = (CourseApplication) result.getModelAndView().getModel().get("courseApplication");
                    assertEquals(courseId, data.getCourseId());
                    assertEquals(title, data.getCourseTitle());
                    assertEquals(fee, data.getFee());
                });
        assertEquals(0, applications.count());
    }

    @Test
    void membershipIsUniqueAndDoesNotCreateOrModifyACourse() {
        Course fixed = records.findByActiveTrueOrderByTitleAscCategory_NameAscProvider_NameAsc().get(0);
        long courseCount = records.count();
        double fee = fixed.getFee();
        assertEquals(0, courses.count());
        assertNull(courseService.addCommonCourse(fixed.getId()));
        assertNotNull(courseService.addCommonCourse(fixed.getId()));
        entityManager.flush(); entityManager.clear();
        assertEquals(1, courses.count());
        assertEquals(courseCount, records.count());
        assertEquals(fee, records.findById(fixed.getId()).orElseThrow().getFee());
    }

    @Test
    void catalogueDeletionPreservesApplicationAndBlocksCategoryDeletion() {
        assertNull(categoryService.createCategory("WORKSHOP", "Workshop", true));
        Category category = categories.findByNameIgnoreCase("WORKSHOP").orElseThrow();
        TrainingProvider provider = providers.save(new TrainingProvider("Catalogue provider"));
        CommonCourse course = courses.saveAndFlush(new CommonCourse(records.save(new Course("Java", category, provider, 200))));
        course.setIntroduction("Java introduction");
        CourseApplication application = new CourseApplication();
        application.setEmployee(employees.findByUsername("emp1").orElseThrow());
        application.setCourseTitle("Java"); application.setProvider(provider);
        application.setCategory(category); application.setFee(150);
        applications.saveAndFlush(application);
        Long id = application.getId();
        assertNotNull(categoryService.deleteCategory(category.getId()));
        assertNull(courseService.deleteCommonCourse(course.getId()));
        provider.setName("Provider renamed");
        entityManager.flush(); entityManager.clear();
        CourseApplication saved = applications.findById(id).orElseThrow();
        assertEquals("Java", saved.getCourseTitle());
        assertEquals("Provider renamed", saved.getProviderName());
        assertEquals(provider.getId(), saved.getProviderId());
        assertNotNull(providerService.deleteProvider(provider.getId()));
        assertTrue(providers.existsById(provider.getId()));
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
    void categoryRelationshipAndFixedFeeSurviveReload() {
        Category category = categories.findByNameIgnoreCase("INTERNAL").orElseThrow();
        TrainingProvider provider = providers.save(new TrainingProvider("Fee provider"));
        CommonCourse course = new CommonCourse("Free internal course", category, provider, 0);
        course.setIntroduction("Recorded cost, excluded from entitlement budget.");
        records.save(course.getCourse());
        courses.saveAndFlush(course);
        Long id = course.getId(); entityManager.clear();
        CommonCourse saved = courses.findById(id).orElseThrow();
        assertEquals(0, saved.getFee());
        assertEquals("INTERNAL", saved.getCategory().getName());
        assertTrue(saved.getCategory().isHalfDayAllowed());
        assertTrue(saved.getIntroduction().contains("excluded"));
    }
}
