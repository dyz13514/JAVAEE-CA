package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.*;
import com.group5.cats.service.NotificationHistoryService;

class AdminNotificationControllerTests {
    private MockHttpSession session(EmployeeRole role) {
        Employee employee = new Employee();
        employee.setRole(role);
        var session = new MockHttpSession();
        session.setAttribute("loggedInUser", employee);
        return session;
    }

    @Test
    void anonymousAndNonAdminUsersCannotReadEmailHistory() throws Exception {
        var service = mock(NotificationHistoryService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminNotificationController(service)).build();
        mvc.perform(get("/admin/notifications")).andExpect(redirectedUrl("/admin/login"));
        for (EmployeeRole role : List.of(EmployeeRole.MANAGER, EmployeeRole.REGULAR_STAFF)) {
            mvc.perform(get("/admin/notifications").session(session(role))).andExpect(redirectedUrl("/employee/home"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void adminCanFilterAndPaginateAndInvalidParametersAreRejected() throws Exception {
        var service = mock(NotificationHistoryService.class);
        var page = new PageImpl<NotificationOutbox>(List.of());
        when(service.findNotifications(DeliveryStatus.FAILED, 1)).thenReturn(page);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminNotificationController(service)).build();
        mvc.perform(get("/admin/notifications").session(session(EmployeeRole.ADMIN))
                .param("status", "FAILED").param("page", "1"))
                .andExpect(status().isOk()).andExpect(view().name("admin/notifications"))
                .andExpect(model().attribute("notifications", page))
                .andExpect(model().attribute("selectedStatus", "FAILED"));
        for (String value : List.of("-1", "10001", "invalid")) {
            mvc.perform(get("/admin/notifications").session(session(EmployeeRole.ADMIN)).param("page", value))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/admin/notifications").session(session(EmployeeRole.ADMIN)).param("status", "invalid"))
                .andExpect(status().isBadRequest());
        verify(service, times(1)).findNotifications(any(), anyInt());
    }
}
