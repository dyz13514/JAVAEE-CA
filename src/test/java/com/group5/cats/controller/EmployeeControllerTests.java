package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.service.CourseApplicationService;

class EmployeeControllerTests {
    private CourseApplicationService service;
    private MockMvc mvc;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = mock(CourseApplicationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EmployeeController(service)).build();
        employee = new Employee();
        employee.setId(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", ""})
    void invalidFeeReturnsFormWithEnteredText(String fee) throws Exception {
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "My course").param("fee", fee))
                .andExpect(status().isOk()).andExpect(view().name("apply-course"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("formAction", "/employee/apply"))
                .andExpect(result -> assertEquals("My course", ((CourseApplication)
                        result.getModelAndView().getModel().get("courseApplication")).getCourseTitle()));
        verifyNoInteractions(service);
    }

    @Test
    void businessValidationKeepsSubmittedFields() throws Exception {
        when(service.submitApplication(any(), eq(employee))).thenReturn("Justification is required.");
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "My course").param("fee", "100"))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attribute("errorMessage", "Justification is required."))
                .andExpect(result -> assertEquals("My course", ((CourseApplication)
                        result.getModelAndView().getModel().get("courseApplication")).getCourseTitle()));
    }

    @Test
    void editBindingFailureKeepsEditAddressAndOriginalId() throws Exception {
        mvc.perform(post("/employee/history/10/edit").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "Updated title").param("fromDate", "invalid-date"))
                .andExpect(status().isOk()).andExpect(view().name("apply-course"))
                .andExpect(model().attribute("formAction", "/employee/history/10/edit"))
                .andExpect(result -> assertEquals(10L, ((CourseApplication)
                        result.getModelAndView().getModel().get("courseApplication")).getId()));
        verifyNoInteractions(service);
    }

    @Test
    void serverControlledFieldsCannotBeBoundFromRequest() throws Exception {
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "My course").param("id", "99")
                .param("employee.id", "99").param("status", "APPROVED")
                .param("trainingDays", "0").param("managerComment", "Approved"))
                .andExpect(status().is3xxRedirection());
        ArgumentCaptor<CourseApplication> input = ArgumentCaptor.forClass(CourseApplication.class);
        verify(service).submitApplication(input.capture(), eq(employee));
        assertNull(input.getValue().getId());
        assertNull(input.getValue().getEmployee());
        assertEquals("APPLIED", input.getValue().getStatus());
        assertNull(input.getValue().getManagerComment());
    }
}
