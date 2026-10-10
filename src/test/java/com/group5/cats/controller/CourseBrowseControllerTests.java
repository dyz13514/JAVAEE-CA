package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

class CourseBrowseControllerTests {
    @Test
    void catalogueRequiresLoginAndPassesFilters() throws Exception {
        CommonCourseService courses = mock(CommonCourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CourseBrowseController(courses,
                mock(CategoryService.class), mock(TrainingProviderService.class)))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
        mvc.perform(get("/employee/courses")).andExpect(redirectedUrl("/employee/login"));
        Employee staff = new Employee();
        staff.setRole(EmployeeRole.REGULAR_STAFF);
        when(courses.findCommonCourses("Java", 2L, 3L)).thenReturn(List.of());
        mvc.perform(get("/employee/courses").sessionAttr("loggedInUser", staff)
                .param("keyword", "Java").param("categoryId", "2").param("providerId", "3"))
                .andExpect(view().name("courses")).andExpect(model().attribute("canApply", true));
        verify(courses).findCommonCourses("Java", 2L, 3L);
    }

    @Test
    void missingEntryIs404AndAdminCannotApply() throws Exception {
        CommonCourseService courses = mock(CommonCourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CourseBrowseController(courses,
                mock(CategoryService.class), mock(TrainingProviderService.class))).build();
        Employee admin = new Employee();
        admin.setRole(EmployeeRole.ADMIN);
        mvc.perform(get("/employee/courses/99").sessionAttr("loggedInUser", admin)).andExpect(status().isNotFound());
        CommonCourse course = new CommonCourse();
        when(courses.findCommonCourseById(1L)).thenReturn(course);
        mvc.perform(get("/employee/courses/1").sessionAttr("loggedInUser", admin))
                .andExpect(view().name("course-detail")).andExpect(model().attribute("canApply", false));
        mvc.perform(get("/admin/courses").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"));
    }
}
