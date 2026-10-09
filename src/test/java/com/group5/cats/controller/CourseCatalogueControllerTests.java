package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.*;

class CourseCatalogueControllerTests {
    private CourseService courses;
    private CommonCourseService common;
    private MockMvc mvc;
    private Employee admin;

    @BeforeEach
    void setUp() {
        courses = mock(CourseService.class);
        common = mock(CommonCourseService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AdminCourseController(courses, mock(TrainingProviderService.class)),
                new CourseBrowseController(courses, mock(TrainingProviderService.class), mock(CourseScheduleService.class)),
                new AdminCommonCourseController(common, courses))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
        admin = new Employee();
        admin.setId(1L);
        admin.setRole(EmployeeRole.ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/common-courses"})
    void bothCataloguesRequireAnAdmin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(redirectedUrl("/admin/login"));
        Employee staff = new Employee();
        staff.setRole(EmployeeRole.REGULAR_STAFF);
        mvc.perform(get(path).sessionAttr("loggedInUser", staff))
                .andExpect(redirectedUrl("/employee/home"));
        mvc.perform(post(path + "/save").sessionAttr("loggedInUser", staff))
                .andExpect(redirectedUrl("/employee/home"));
        verifyNoInteractions(courses, common);
    }

    @Test
    void adminCatalogueDisplaysManagementCards() throws Exception {
        mvc.perform(get("/admin/courses").sessionAttr("loggedInUser", admin))
                .andExpect(view().name("courses"))
                .andExpect(model().attribute("canApply", false));
    }

    @Test
    void commonSelectionBindsOnlyACourseId() throws Exception {
        mvc.perform(post("/admin/common-courses/save").sessionAttr("loggedInUser", admin)
                .param("courseId", "12").param("title", "Ignored title"))
                .andExpect(redirectedUrl("/admin/common-courses"));
        verify(common).createCommonCourse(argThat(form -> Long.valueOf(12).equals(form.getCourseId())));
        verify(courses, never()).createCourse(any());
    }

    @Test
    void malformedCommonSelectionRedisplaysForm() throws Exception {
        mvc.perform(post("/admin/common-courses/save").sessionAttr("loggedInUser", admin)
                .param("courseId", "invalid"))
                .andExpect(view().name("admin/common-course-form"))
                .andExpect(model().attributeExists("errorMessage", "courses"));
        verify(common, never()).createCommonCourse(any());
    }

    @Test
    void totalCourseValidationErrorKeepsProviderOptions() throws Exception {
        when(courses.createCourse(any())).thenReturn("Course title is required.");
        mvc.perform(post("/admin/courses/save").sessionAttr("loggedInUser", admin)
                .param("fee", "0"))
                .andExpect(view().name("admin/course-form"))
                .andExpect(model().attributeExists("errorMessage", "providers"));
    }
}
