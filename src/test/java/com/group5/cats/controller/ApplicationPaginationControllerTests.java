package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import com.group5.cats.dto.ApplicationSearch;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

class ApplicationPaginationControllerTests {
    @Test
    void historyPassesNormalizedSearchAndOnlyQueriesTheLoggedInEmployee() throws Exception {
        var service = mock(CourseApplicationService.class);
        var controller = new EmployeeController(service, mock(EntitlementService.class),
                mock(CourseService.class), mock(CourseScheduleService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html")).build();
        Employee user = user(EmployeeRole.REGULAR_STAFF);
        CourseApplication row = new CourseApplication();
        when(service.searchEmployeeApplications(eq(user), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(1, 20), 35));
        mvc.perform(get("/employee/history")).andExpect(redirectedUrl("/employee/login"));
        verifyNoInteractions(service);
        mvc.perform(get("/employee/history").sessionAttr("loggedInUser", user)
                .param("keyword", " Java ").param("page", "2").param("size", "20")
                .param("employeeId", "999"))
                .andExpect(model().attribute("applications", List.of(row)))
                .andExpect(model().attributeExists("pagination"));
        var search = ArgumentCaptor.forClass(ApplicationSearch.class);
        verify(service).searchEmployeeApplications(eq(user), eq(true), search.capture());
        assertEquals("Java", search.getValue().getKeyword());
        assertEquals(2, search.getValue().getPage());
        assertEquals(20, search.getValue().getSize());
        mvc.perform(get("/employee/history").sessionAttr("loggedInUser", user)
                .param("page", "invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void teamListRequiresManagerAndReviewReturnsToTheSameSearchPage() throws Exception {
        var service = mock(CourseApplicationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new ManagerController(service))
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html")).build();
        Employee manager = user(EmployeeRole.MANAGER);
        when(service.searchTeamApplications(eq(manager), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/manager/approvals").sessionAttr("loggedInUser", user(EmployeeRole.REGULAR_STAFF)))
                .andExpect(redirectedUrl("/employee/home"));
        verifyNoInteractions(service);
        mvc.perform(get("/manager/approvals").sessionAttr("loggedInUser", manager)
                .param("page", "-1").param("size", "500"))
                .andExpect(model().attributeExists("pagination"));
        var search = ArgumentCaptor.forClass(ApplicationSearch.class);
        verify(service).searchTeamApplications(eq(manager), search.capture());
        assertEquals(1, search.getValue().getPage());
        assertEquals(10, search.getValue().getSize());
        when(service.reviewApplication(8L, manager, "APPROVE", "Useful training"))
                .thenReturn("Approved");
        mvc.perform(post("/manager/review/8").sessionAttr("loggedInUser", manager)
                .param("decision", "APPROVE").param("comment", "Useful training")
                .param("page", "2").param("size", "25").param("keyword", "Java"))
                .andExpect(redirectedUrl("/manager/approvals?page=2&size=25&keyword=Java"));
    }

    private Employee user(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setRole(role);
        return employee;
    }
}
