package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.dto.RegistrationForm;
import com.group5.cats.model.*;
import com.group5.cats.service.*;

class RegistrationControllerTests {
    private RegistrationService service;
    private MockMvc mvc;
    private MockHttpSession session;
    @BeforeEach void setup() {
        service = mock(RegistrationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RegistrationController(service, mock(EmployeeService.class))).build();
        session = new MockHttpSession(); session.setAttribute("registrationToken", "test-token");
    }
    @Test void registrationRequiresTheSessionToken() throws Exception {
        mvc.perform(post("/register").session(session).param("username", "new.staff"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void successfulSubmissionReturnsToLoginWithoutAuthenticating() throws Exception {
        mvc.perform(post("/register").session(session).param("registrationToken", "test-token")
                .param("name", "New Staff").param("username", "new.staff").param("password", "test-password")
                .param("confirmPassword", "test-password").param("role", "ADMIN"))
                .andExpect(redirectedUrl("/employee/login")).andExpect(flash().attributeExists("successMessage"))
                .andExpect(request().sessionAttributeDoesNotExist("loggedInUser"));
        var captured = org.mockito.ArgumentCaptor.forClass(RegistrationForm.class);
        verify(service).submit(captured.capture()); assertEquals("new.staff", captured.getValue().getUsername());
    }
    @Test void invalidSubmissionClearsPasswordsAndKeepsTheForm() throws Exception {
        doThrow(new IllegalArgumentException("Passwords do not match.")).when(service).submit(any());
        var result = mvc.perform(post("/register").session(session).param("registrationToken", "test-token")
                .param("name", "New Staff").param("username", "new.staff").param("password", "test-password")
                .param("confirmPassword", "wrong-password"))
                .andExpect(view().name("login")).andExpect(model().attribute("accountMode", "register"))
                .andReturn();
        RegistrationForm form = (RegistrationForm) result.getModelAndView().getModel().get("registrationForm");
        assertNull(form.getPassword()); assertNull(form.getConfirmPassword()); assertEquals("new.staff", form.getUsername());
    }
    @Test void onlyAdminsCanReadOrReviewRequests() throws Exception {
        mvc.perform(get("/admin/registrations")).andExpect(redirectedUrl("/admin/login"));
        Employee staff = new Employee(); staff.setRole(EmployeeRole.REGULAR_STAFF); session.setAttribute("loggedInUser", staff);
        mvc.perform(get("/admin/registrations").session(session)).andExpect(redirectedUrl("/employee/home"));
        mvc.perform(post("/admin/registrations/1/approve").session(session).param("registrationToken", "test-token"))
                .andExpect(redirectedUrl("/employee/home"));
        mvc.perform(post("/admin/registrations/1/reject").session(session).param("reason", "Invalid details"))
                .andExpect(redirectedUrl("/employee/home"));
        verifyNoInteractions(service);
    }
    @Test void adminReviewAlsoRequiresTheTokenAndUsesTheLoggedInReviewer() throws Exception {
        Employee admin = new Employee(); admin.setRole(EmployeeRole.ADMIN); session.setAttribute("loggedInUser", admin);
        mvc.perform(post("/admin/registrations/1/approve").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/registrations/1/approve").session(session).param("registrationToken", "test-token")
                .param("designation", "PROFESSIONAL").param("supervisorId", "2"))
                .andExpect(redirectedUrl("/admin/registrations"));
        verify(service).approve(1L, EmployeeDesignation.PROFESSIONAL, 2L, admin);
    }
}
