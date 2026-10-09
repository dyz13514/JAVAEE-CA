package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.*;
import com.group5.cats.service.CourseService;

class CourseBrowseControllerTests {
    @Test
    void commonCatalogueContainsOnlyAdminSelectedCoursesAndRequiresLogin() throws Exception {
        CourseService service = mock(CourseService.class);
        Course first = new Course(); first.setId(1L);
        Course second = new Course(); second.setId(2L);
        when(service.findCourses("", "", null, false)).thenReturn(List.of(first, second));
        when(service.findCourses("", "", null, true)).thenReturn(List.of(second));
        when(service.findCommonCourseIds()).thenReturn(List.of(2L));
        when(service.findAllCourses()).thenReturn(List.of(first, second));
        var mvc = MockMvcBuilders.standaloneSetup(new CourseBrowseController(service, mock(com.group5.cats.service.TrainingProviderService.class), mock(com.group5.cats.service.CourseScheduleService.class)))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
        mvc.perform(get("/courses")).andExpect(redirectedUrl("/employee/login"));
        verifyNoInteractions(service);
        Employee staff = new Employee(); staff.setRole(EmployeeRole.REGULAR_STAFF);
        mvc.perform(get("/employee/courses").param("commonOnly", "true").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("courses", List.of(second)))
                .andExpect(model().attribute("canApply", true))
                .andExpect(model().attribute("featuredCourses", List.of(second)));
        mvc.perform(get("/employee/courses").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("courses", List.of(first, second)));
        staff.setRole(EmployeeRole.MANAGER);
        mvc.perform(get("/employee/courses").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("canApply", true));
    }
    @Test
    void roleSpecificUrlsAndDetailPermissionsAreConsistent() throws Exception {
        CourseService service = mock(CourseService.class);
        Course course = new Course();
        course.setId(7L);
        when(service.findCourseById(7L)).thenReturn(course);
        var mvc = MockMvcBuilders.standaloneSetup(new CourseBrowseController(service,
                mock(com.group5.cats.service.TrainingProviderService.class),
                mock(com.group5.cats.service.CourseScheduleService.class)))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
        for (EmployeeRole role : EmployeeRole.values()) {
            Employee user = new Employee(); user.setRole(role);
            String prefix = role == EmployeeRole.ADMIN ? "/admin" : "/employee";
            mvc.perform(get("/courses").param("keyword", "Java").sessionAttr("loggedInUser", user))
                    .andExpect(redirectedUrl(prefix + "/courses?keyword=Java"));
            mvc.perform(get(prefix + "/courses/7").sessionAttr("loggedInUser", user))
                    .andExpect(model().attribute("canApply", role != EmployeeRole.ADMIN));
            mvc.perform(get("/courses/7").sessionAttr("loggedInUser", user))
                    .andExpect(redirectedUrl(prefix + "/courses/7"));
        }
    }

}
