package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

class CourseBrowseControllerTests {
    @Test
    void employeeAndManagerReceiveBothFilteredListsWithoutManagementActions() throws Exception {
        CommonCourseService common = mock(CommonCourseService.class);
        CourseService courses = mock(CourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CourseBrowseController(common,
                mock(CategoryService.class), mock(TrainingProviderService.class), courses))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
        mvc.perform(get("/employee/courses")).andExpect(redirectedUrl("/employee/login"));
        Course course = new Course(); course.setId(8L);
        when(common.findCommonCourses("Java", 2L, 3L)).thenReturn(List.of(new CommonCourse(course)));
        when(courses.findCourses("Java", 2L, 3L)).thenReturn(List.of(course));
        for (EmployeeRole role : List.of(EmployeeRole.REGULAR_STAFF, EmployeeRole.MANAGER)) {
            Employee staff = new Employee(); staff.setRole(role);
            mvc.perform(get("/employee/courses").sessionAttr("loggedInUser", staff)
                    .param("keyword", "Java").param("categoryId", "2").param("providerId", "3"))
                    .andExpect(view().name("courses")).andExpect(model().attribute("canApply", true))
                    .andExpect(model().attribute("featuredCourses", List.of(course)))
                    .andExpect(model().attribute("courses", List.of(course)));
        }
    }

    @Test
    void allCourseDetailsWorkWithoutCommonMembershipAndLegacyLinksResolveCorrectly() throws Exception {
        CommonCourseService common = mock(CommonCourseService.class);
        CourseService courses = mock(CourseService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CourseBrowseController(common,
                mock(CategoryService.class), mock(TrainingProviderService.class), courses)).build();
        Employee admin = new Employee(); admin.setRole(EmployeeRole.ADMIN);
        mvc.perform(get("/employee/courses/details/99").sessionAttr("loggedInUser", admin)).andExpect(status().isNotFound());
        Course course = new Course(); course.setId(8L);
        when(courses.findCourseById(8L)).thenReturn(Optional.of(course));
        mvc.perform(get("/employee/courses/details/8").sessionAttr("loggedInUser", admin))
                .andExpect(view().name("course-detail")).andExpect(model().attribute("canApply", false))
                .andExpect(model().attribute("course", course));
        when(common.findCommonCourseById(1L)).thenReturn(new CommonCourse(course));
        mvc.perform(get("/employee/courses/1").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/employee/courses/details/8"));
        mvc.perform(get("/admin/courses").sessionAttr("loggedInUser", admin))
                .andExpect(redirectedUrl("/admin/common-courses"));
    }
}
