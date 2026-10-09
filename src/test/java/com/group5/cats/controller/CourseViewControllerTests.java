package com.group5.cats.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import com.group5.cats.model.*;
import com.group5.cats.repository.*;
import com.group5.cats.service.TrainingCalendarServiceImpl;

class CourseViewControllerTests {
    @Test
    void roleMatrixAlsoProtectsDirectEmployeeUrls() throws Exception {
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CourseApplicationRepository applications = mock(CourseApplicationRepository.class);
        var applicationService = mock(com.group5.cats.service.CourseApplicationService.class);
        when(applicationService.searchEmployeeApplications(any(), eq(false), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        Employee manager = employee(1L, EmployeeRole.MANAGER, null);
        Employee staff = employee(2L, EmployeeRole.REGULAR_STAFF, manager);
        Employee outsider = employee(3L, EmployeeRole.REGULAR_STAFF, null);
        Employee admin = employee(4L, EmployeeRole.ADMIN, manager);
        for (Employee user : List.of(manager, staff, outsider, admin)) {
            when(employees.findById(user.getId())).thenReturn(Optional.of(user));
        }
        when(employees.findBySupervisor_Id(1L)).thenReturn(List.of(staff, admin));
        when(employees.findAll()).thenReturn(List.of(manager, staff, outsider, admin));
        CourseApplication pending = new CourseApplication();
        pending.setStatus("APPLIED");
        when(applicationService.searchEmployeeApplications(eq(staff), eq(false), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(pending)));
        var mvc = MockMvcBuilders.standaloneSetup(new CourseViewController(employees, applicationService,
                new TrainingCalendarServiceImpl(applications, employees)))
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html")).build();
        mvc.perform(get("/course-view")).andExpect(redirectedUrl("/employee/login"));
        mvc.perform(get("/course-view").sessionAttr("loggedInUser", staff))
                .andExpect(model().attribute("applications", List.of(pending)));
        mvc.perform(get("/course-view").param("employeeId", "1").sessionAttr("loggedInUser", staff))
                .andExpect(status().isForbidden());
        mvc.perform(get("/course-view").param("employeeId", "2").sessionAttr("loggedInUser", manager))
                .andExpect(model().attribute("applications", List.of(pending)));
        mvc.perform(get("/course-view").param("employeeId", "3").sessionAttr("loggedInUser", manager))
                .andExpect(status().isForbidden());
        mvc.perform(get("/course-view").param("others", "true").sessionAttr("loggedInUser", manager))
                .andExpect(model().attribute("employees", List.of(staff, admin)));
        mvc.perform(get("/course-view").param("employeeId", "3").sessionAttr("loggedInUser", admin))
                .andExpect(status().isOk());
        mvc.perform(get("/course-view").param("others", "true").sessionAttr("loggedInUser", staff))
                .andExpect(status().isForbidden());
    }

    private Employee employee(Long id, EmployeeRole role, Employee supervisor) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setRole(role);
        employee.setSupervisor(supervisor);
        return employee;
    }
}
