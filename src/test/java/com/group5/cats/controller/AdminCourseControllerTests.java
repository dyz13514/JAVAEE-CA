package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.*;

class AdminCourseControllerTests {
    @Test
    void creationAndDeletionRequireAdminOnTheServer() throws Exception {
        CourseService courses = mock(CourseService.class);
        CategoryService categories = mock(CategoryService.class);
        TrainingProviderService providers = mock(TrainingProviderService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminCourseController(courses, categories, providers)).build();
        mvc.perform(get("/admin/courses/new")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/courses/save")).andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/courses/1/delete")).andExpect(redirectedUrl("/admin/login"));
        for (EmployeeRole role : List.of(EmployeeRole.REGULAR_STAFF, EmployeeRole.MANAGER)) {
            Employee employee = new Employee();
            employee.setRole(role);
            mvc.perform(get("/admin/courses/new").sessionAttr("loggedInUser", employee)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/courses/save").sessionAttr("loggedInUser", employee)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/courses/1/delete").sessionAttr("loggedInUser", employee)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(courses, categories, providers);
    }
}
