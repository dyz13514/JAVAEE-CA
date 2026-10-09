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
        mvc.perform(get("/courses").param("commonOnly", "true").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("courses", List.of(second)))
                .andExpect(model().attribute("canApply", true))
                .andExpect(model().attribute("featuredCourses", List.of(second)));
        mvc.perform(get("/courses").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("courses", List.of(first, second)));
        staff.setRole(EmployeeRole.MANAGER);
        mvc.perform(get("/courses").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("canApply", true));
    }
}
