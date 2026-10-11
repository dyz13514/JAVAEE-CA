package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.EntitlementSummary;
import com.group5.cats.service.CourseApplicationService;
import com.group5.cats.service.EntitlementService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class EmployeeControllerTests {
    private CourseApplicationService service;
    private EntitlementService entitlementService;
    private MockMvc mvc;
    private com.group5.cats.service.CommonCourseService courses;
    private com.group5.cats.service.CourseService library;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = mock(CourseApplicationService.class);
        entitlementService = mock(EntitlementService.class);
        courses = mock(com.group5.cats.service.CommonCourseService.class);
        library = mock(com.group5.cats.service.CourseService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EmployeeController(service, entitlementService, courses, mock(com.group5.cats.service.CategoryService.class), mock(com.group5.cats.service.TrainingProviderService.class), library)).build();
        employee = new Employee();
        employee.setId(1L);
        employee.setRole(EmployeeRole.REGULAR_STAFF);
    }

    @Test
    void allCourseLinkPrefillsEvenWithoutCommonMembership() throws Exception {
        var course = new com.group5.cats.model.Course("Java", com.group5.cats.CategoryFixtures.category("EXTERNAL"),
                new com.group5.cats.model.TrainingProvider("AWS"), 300);
        course.setId(8L);
        when(library.findCourseById(8L)).thenReturn(Optional.of(course));
        mvc.perform(get("/employee/apply").param("courseId", "8").sessionAttr("loggedInUser", employee))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attribute("courseLocked", true))
                .andExpect(model().attribute("lockedCourseId", 8L))
                .andExpect(result -> {
                    CourseApplication data = (CourseApplication) result.getModelAndView().getModel().get("courseApplication");
                    assertEquals(8L, data.getCourseId());
                    assertEquals("Java", data.getCourseTitle());
                    assertEquals("AWS", data.getProviderName());
                    assertEquals(300, data.getFee());
                    assertNull(data.getId());
                });
        verifyNoInteractions(courses, service);
        mvc.perform(get("/employee/apply").param("courseId", "999").sessionAttr("loggedInUser", employee))
                .andExpect(redirectedUrl("/employee/courses"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void lockedSelectionSurvivesValidationAndOverridesAnotherSubmittedCourseId() throws Exception {
        when(service.submitApplication(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(employee)))
                .thenReturn("Justification is required.");
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("lockedCourseId", "8").param("courseId", "99"))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attribute("courseLocked", true))
                .andExpect(model().attribute("lockedCourseId", 8L))
                .andExpect(result -> {
                    CourseApplication data = (CourseApplication) result.getModelAndView().getModel().get("courseApplication");
                    assertEquals(8L, data.getCourseId());
                });
        mvc.perform(get("/employee/apply").sessionAttr("loggedInUser", employee))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attributeDoesNotExist("lockedCourseId"));
    }

    @Test
    void courseLinkPrefillsANewApplicationWithoutSavingIt() throws Exception {
        var course = new com.group5.cats.model.CommonCourse("Java", com.group5.cats.CategoryFixtures.category("EXTERNAL"),
                new com.group5.cats.model.TrainingProvider("NUS-ISS"), 250);
        course.setId(7L);
        course.getProvider().setId(3L);
        when(courses.findCommonCourseById(7L)).thenReturn(course);
        mvc.perform(get("/employee/apply").param("commonCourseId", "7").sessionAttr("loggedInUser", employee))
                .andExpect(view().name("apply-course"))
                .andExpect(result -> {
                    CourseApplication data = (CourseApplication) result.getModelAndView().getModel().get("courseApplication");
                    assertEquals("EXTERNAL", data.getCategoryName());
                    assertEquals("Java", data.getCourseTitle());
                    assertEquals("NUS-ISS", data.getProviderName());
                    assertSame(course.getProvider(), data.getProvider());
                    assertEquals(250, data.getFee());
                    assertEquals(3L, data.getProviderId());
                    assertNull(data.getId());
                });
        verifyNoInteractions(service);
    }

    @Test
    void selectedProviderIdIsBoundAndPreservedWhenServiceRejectsSubmission() throws Exception {
        when(service.submitApplication(any(), eq(employee))).thenReturn("Selected training provider no longer exists.");
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "Java").param("categoryId", "2")
                .param("providerId", "3").param("provider.id", "999").param("provider.name", "Forged").param("fee", "100"))
                .andExpect(view().name("apply-course"))
                .andExpect(result -> {
                    CourseApplication data = (CourseApplication) result.getModelAndView()
                            .getModel().get("courseApplication");
                    assertEquals(3L, data.getProviderId());
                    assertNull(data.getProvider());
                });
        verify(service).submitApplication(argThat(data -> Long.valueOf(3L).equals(data.getProviderId())), eq(employee));
    }

    @Test
    void malformedProviderIdReturnsTheFormWithoutCallingService() throws Exception {
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("providerId", "abc").param("fee", "100"))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attributeHasFieldErrors("courseApplication", "providerId"));
        verifyNoInteractions(service);
    }

    @Test
    void directApplicationLinkRequiresLoginAndHandlesDeletedCourse() throws Exception {
        mvc.perform(get("/employee/apply").param("commonCourseId", "7"))
                .andExpect(redirectedUrl("/employee/login"));
        mvc.perform(get("/employee/apply").param("commonCourseId", "7").sessionAttr("loggedInUser", employee))
                .andExpect(redirectedUrl("/employee/courses"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void overviewUsesOwnAllowanceAndThreeNewestApplications() throws Exception {
        int year = LocalDate.now().getYear();
        EntitlementSummary allowance = new EntitlementSummary();
        when(entitlementService.getEntitlementSummary(1L, year)).thenReturn(Optional.of(allowance));
        List<CourseApplication> applications = List.of(1L, 4L, 2L, 3L).stream().map(id -> {
            CourseApplication application = new CourseApplication();
            application.setId(id);
            return application;
        }).toList();
        when(service.findApplicationsByEmployee(employee)).thenReturn(applications);
        mvc.perform(get("/employee/home").sessionAttr("loggedInUser", employee))
                .andExpect(view().name("employee-home"))
                .andExpect(model().attribute("allowance", allowance))
                .andExpect(model().attribute("entitlementYear", year))
                .andExpect(model().attribute("recentApplications",
                        List.of(applications.get(1), applications.get(3), applications.get(2))));
    }

    @Test
    void overviewAllowsMissingAllowanceAndRedirectsAdminsBeforeQuerying() throws Exception {
        when(entitlementService.getEntitlementSummary(eq(1L), anyInt())).thenReturn(Optional.empty());
        when(service.findApplicationsByEmployee(employee)).thenReturn(List.of());
        mvc.perform(get("/employee/home").sessionAttr("loggedInUser", employee))
                .andExpect(view().name("employee-home"))
                .andExpect(model().attribute("recentApplications", List.of()));
        clearInvocations(service, entitlementService);
        employee.setRole(EmployeeRole.ADMIN);
        mvc.perform(get("/employee/home").sessionAttr("loggedInUser", employee))
                .andExpect(redirectedUrl("/admin/home"));
        verifyNoInteractions(service, entitlementService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-id"})
    void invalidCourseIdReturnsFormWithEnteredText(String courseId) throws Exception {
        mvc.perform(post("/employee/apply").param("commonCourseId", "7").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "My course").param("courseId", courseId))
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
        mvc.perform(post("/employee/apply").param("commonCourseId", "7").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "My course").param("fee", "100"))
                .andExpect(view().name("apply-course"))
                .andExpect(model().attribute("errorMessage", "Justification is required."))
                .andExpect(result -> assertEquals("My course", ((CourseApplication)
                        result.getModelAndView().getModel().get("courseApplication")).getCourseTitle()));
    }

    @Test
    void editBindingFailureKeepsEditAddressAndOriginalId() throws Exception {
        mvc.perform(post("/employee/history/10/edit").param("commonCourseId", "7").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "Updated title").param("fromDate", "invalid-date"))
                .andExpect(status().isOk()).andExpect(view().name("apply-course"))
                .andExpect(model().attribute("formAction", "/employee/history/10/edit"))
                .andExpect(result -> assertEquals(10L, ((CourseApplication)
                        result.getModelAndView().getModel().get("courseApplication")).getId()));
        verifyNoInteractions(service);
    }

    @Test
    void serverControlledFieldsCannotBeBoundFromRequest() throws Exception {
        mvc.perform(post("/employee/apply").param("commonCourseId", "7").sessionAttr("loggedInUser", employee)
                .param("courseTitle", "My course").param("id", "99")
                .param("employee.id", "99").param("status", "APPROVED")
                .param("trainingDays", "0").param("managerComment", "Approved")
                .param("fee", "1").param("provider", "Forged provider"))
                .andExpect(status().is3xxRedirection());
        ArgumentCaptor<CourseApplication> input = ArgumentCaptor.forClass(CourseApplication.class);
        verify(service).submitApplication(input.capture(), eq(employee));
        assertNull(input.getValue().getId());
        assertNull(input.getValue().getEmployee());
        assertEquals("APPLIED", input.getValue().getStatus());
        assertNull(input.getValue().getManagerComment());
        assertEquals(0, input.getValue().getFee());
        assertNull(input.getValue().getProvider());
    }
    @Test
    void selectedCourseIdIsPassedToApplicationService() throws Exception {
        mvc.perform(post("/employee/apply").sessionAttr("loggedInUser", employee)
                .param("courseId", "12").param("courseTitle", "Java").param("categoryId", "2")
                .param("fromDate", "2027-01-04").param("toDate", "2027-01-05").param("fee", "100"))
                .andExpect(redirectedUrl("/employee/history"));
        verify(service).submitApplication(argThat(data -> data.getCourseId().equals(12L)), eq(employee));
    }

    @Test
    void anotherEmployeesApplicationCannotBeViewedOrEdited() throws Exception {
        CourseApplication application = new CourseApplication();
        Employee owner = new Employee();
        owner.setId(9L);
        application.setEmployee(owner);
        when(service.findApplicationById(10L)).thenReturn(Optional.of(application));
        mvc.perform(get("/employee/history/10").sessionAttr("loggedInUser", employee))
                .andExpect(status().isForbidden());
        mvc.perform(get("/employee/history/10/edit").sessionAttr("loggedInUser", employee))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotOpenOrSubmitApplicationEndpoint() throws Exception {
        employee.setRole(EmployeeRole.ADMIN);
        for (String path : List.of("/employee/apply")) {
            mvc.perform(get(path).param("commonCourseId", "7").sessionAttr("loggedInUser", employee))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).param("commonCourseId", "7").sessionAttr("loggedInUser", employee))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void managerAndEmployeeShareApplicationAndHomeAddresses() throws Exception {
        for (EmployeeRole role : List.of(EmployeeRole.REGULAR_STAFF, EmployeeRole.MANAGER)) {
            employee.setRole(role);
            mvc.perform(get("/employee/apply").sessionAttr("loggedInUser", employee))
                    .andExpect(view().name("apply-course"));
            mvc.perform(post("/employee/apply").param("commonCourseId", "7").sessionAttr("loggedInUser", employee))
                    .andExpect(redirectedUrl("/employee/history"));
            mvc.perform(get("/employee/home").sessionAttr("loggedInUser", employee))
                    .andExpect(view().name("employee-home"));
        }
        verify(service, times(2)).submitApplication(any(), eq(employee));
    }

}
