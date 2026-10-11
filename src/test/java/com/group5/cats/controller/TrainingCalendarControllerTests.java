package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeDesignation;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.TrainingCalendarServiceImpl;

class TrainingCalendarControllerTests {

    private MockMvc mvc;
    private CourseApplicationRepository applications;
    private EmployeeRepository employees;
    private Employee staff;
    private Employee manager;
    private Employee admin;
    private Employee unrelated;
    private com.group5.cats.service.CourseApplicationService history;

    @BeforeEach
    void setUp() {
        applications = mock(CourseApplicationRepository.class);
        employees = mock(EmployeeRepository.class);
        manager = employee(1L, "Manager One", EmployeeRole.MANAGER);
        staff = employee(2L, "Staff Two", EmployeeRole.REGULAR_STAFF);
        admin = employee(3L, "Admin Three", EmployeeRole.ADMIN);
        unrelated = employee(4L, "Unrelated Four", EmployeeRole.REGULAR_STAFF);
        staff.setSupervisor(manager);
        when(employees.findBySupervisor_Id(manager.getId())).thenReturn(List.of(staff));
        when(employees.findAll()).thenReturn(List.of(manager, staff, admin, unrelated));

        // Render real HTML with the real service; only database access is mocked.
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");

        TrainingCalendarServiceImpl service =
                new TrainingCalendarServiceImpl(applications, employees);
        history = mock(com.group5.cats.service.CourseApplicationService.class);
        when(history.searchEmployeeApplications(eq(staff), eq(true), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        mvc = MockMvcBuilders.standaloneSetup(
                new TrainingCalendarController(service, employees),
                new EmployeeController(history, mock(com.group5.cats.service.EntitlementService.class),
                        mock(com.group5.cats.service.CommonCourseService.class),
                        mock(com.group5.cats.service.CategoryService.class),
                        mock(com.group5.cats.service.TrainingProviderService.class),
                        mock(com.group5.cats.service.CourseService.class)))
                .setViewResolvers(views).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/training-calendar", "/training-calendar/employees"})
    void loginIsRequired(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(applications);
    }

    @ParameterizedTest
    @EnumSource(EmployeeRole.class)
    void staffAndManagerStartWithOwnCalendarWhileAdminGetsManagement(EmployeeRole role) throws Exception {
        staff.setRole(role);
        if (role == EmployeeRole.ADMIN) {
            mvc.perform(get("/training-calendar").sessionAttr("loggedInUser", staff))
                    .andExpect(redirectedUrl("/course-view"));
            verifyNoInteractions(applications);
            return;
        }
        LocalDate today = LocalDate.now();
        MvcResult result = mvc.perform(get("/training-calendar")
                .sessionAttr("loggedInUser", staff))
                .andExpect(status().isOk())
                .andExpect(model().attribute("calendarEmployee", staff))
                .andExpect(model().attribute("isOwnCalendar", true))
                .andExpect(model().attribute("selectedYear", today.getYear()))
                .andExpect(model().attribute("selectedMonth", today.getMonthValue()))
                .andReturn();

        String html = html(result);
        assertTrue(html.contains("Monday"));
        assertTrue(html.contains("/css/cats.css"));
        assertTrue(html.contains("aria-label=\"Main navigation\""));
        assertTrue(html.contains("No approved courses found"));
        assertFalse(html.contains("Others' Calendars"), "The personal calendar stays in My training");
        assertEquals(role == EmployeeRole.MANAGER, html.contains("Team training"));
        verify(applications).findApprovedApplicationsForCalendar(
                staff.getId(), today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    }

    @Test
    void applicationsTabFromCalendarOpensHistoryWithDefaultPaging() throws Exception {
        String calendar = html(mvc.perform(get("/training-calendar")
                .sessionAttr("loggedInUser", staff)).andExpect(status().isOk()).andReturn());
        var link = java.util.regex.Pattern.compile("href=\"([^\"]+)\"[^>]*>Applications</a>").matcher(calendar);
        assertTrue(link.find(), "The calendar must expose the Applications tab");
        String destination = org.springframework.web.util.HtmlUtils.htmlUnescape(link.group(1));
        mvc.perform(get(java.net.URI.create(destination)).sessionAttr("loggedInUser", staff))
                .andExpect(status().isOk()).andExpect(view().name("my-history"));
        var search = org.mockito.ArgumentCaptor.forClass(com.group5.cats.dto.ApplicationSearch.class);
        verify(history).searchEmployeeApplications(eq(staff), eq(true), search.capture());
        assertEquals(10, search.getValue().getSize());
        assertEquals(1, search.getValue().getPage());
    }

    @Test
    void managerListContainsOnlyDirectReportsAndClickableNames() throws Exception {
        MvcResult result = mvc.perform(get("/training-calendar/employees")
                .sessionAttr("loggedInUser", manager))
                .andExpect(status().isOk())
                .andExpect(model().attribute("employees", List.of(staff)))
                .andReturn();
        String html = html(result);
        assertTrue(html.contains("/training-calendar?employeeId=2"));
        assertTrue(html.contains("Staff Two"));
        assertFalse(html.contains("Unrelated Four"));
        assertFalse(html.contains("Admin Three"));
    }

    @Test
    void adminListContainsEveryoneIncludingOwnAccount() throws Exception {
        String html = html(mvc.perform(get("/training-calendar/employees")
                .sessionAttr("loggedInUser", admin))
                .andExpect(status().isOk()).andReturn());
        assertTrue(html.contains("Manager One"));
        assertTrue(html.contains("Staff Two"));
        assertTrue(html.contains("Admin Three"));
        assertTrue(html.contains("Unrelated Four"));
    }

    @Test
    void regularEmployeeCannotOpenThePeopleList() throws Exception {
        String html = html(mvc.perform(get("/training-calendar/employees")
                .sessionAttr("loggedInUser", staff))
                .andExpect(status().isForbidden()).andReturn());
        assertFalse(html.contains("<table"));
        assertFalse(html.contains("Unrelated Four"));
    }

    @Test
    void editingEmployeeIdCannotBypassEmployeeOrManagerAccess() throws Exception {
        for (Employee viewer : List.of(staff, manager)) {
            String html = html(mvc.perform(get("/training-calendar")
                    .param("employeeId", unrelated.getId().toString())
                    .sessionAttr("loggedInUser", viewer))
                    .andExpect(status().isForbidden()).andReturn());
            assertFalse(html.contains("Unrelated Four"));
            assertFalse(html.contains("class=\"course\""));
        }
        verifyNoInteractions(applications);
    }

    @Test
    void missingEmployeeShowsAnAccessErrorWithoutCourses() throws Exception {
        mvc.perform(get("/training-calendar").param("employeeId", "999")
                .sessionAttr("loggedInUser", admin))
                .andExpect(status().isForbidden());
        verifyNoInteractions(applications);
    }

    @Test
    void changedDatabaseRoleOverridesOldSessionRole() throws Exception {
        Employee oldSession = new Employee();
        oldSession.setId(manager.getId());
        oldSession.setRole(EmployeeRole.MANAGER);
        manager.setRole(EmployeeRole.REGULAR_STAFF);

        mvc.perform(get("/training-calendar/employees")
                .sessionAttr("loggedInUser", oldSession))
                .andExpect(status().isForbidden());
        mvc.perform(get("/training-calendar").param("employeeId", staff.getId().toString())
                .sessionAttr("loggedInUser", oldSession))
                .andExpect(status().isForbidden());
        verifyNoInteractions(applications);
    }

    @Test
    void deletedAccountMustLogInAgain() throws Exception {
        when(employees.findById(staff.getId())).thenReturn(Optional.empty());
        mvc.perform(get("/training-calendar").sessionAttr("loggedInUser", staff))
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(applications);
    }

    @Test
    void otherEmployeeStaysSelectedWhenChangingMonth() throws Exception {
        for (int month : List.of(10, 11)) {
            String html = html(mvc.perform(get("/training-calendar")
                    .param("employeeId", staff.getId().toString())
                    .param("year", "2026").param("month", Integer.toString(month))
                    .sessionAttr("loggedInUser", manager))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("calendarEmployee", staff))
                    .andExpect(model().attribute("isOwnCalendar", false))
                    .andReturn());
            assertTrue(html.contains("type=\"hidden\" name=\"employeeId\" value=\"2\""));
            assertTrue(html.contains("Back to Employee List"));
            LocalDate first = LocalDate.of(2026, month, 1);
            verify(applications).findApprovedApplicationsForCalendar(
                    staff.getId(), first, first.withDayOfMonth(first.lengthOfMonth()));
        }
    }

    @Test
    void crossMonthCoursesAppearOnEachCoveredDateOnly() throws Exception {
        CourseApplication course = new CourseApplication();
        course.setEmployee(staff);
        course.setCourseTitle("Cross-month training");
        course.setCategory(com.group5.cats.CategoryFixtures.category("EXTERNAL"));
        course.setFromDate(LocalDate.of(2026, 10, 30));
        course.setToDate(LocalDate.of(2026, 11, 2));
        for (int month : List.of(10, 11)) {
            LocalDate first = LocalDate.of(2026, month, 1);
            when(applications.findApprovedApplicationsForCalendar(
                    staff.getId(), first, first.withDayOfMonth(first.lengthOfMonth())))
                    .thenReturn(List.of(course));
            String html = html(mvc.perform(get("/training-calendar")
                    .param("year", "2026").param("month", Integer.toString(month))
                    .sessionAttr("loggedInUser", staff))
                    .andExpect(status().isOk()).andReturn());
            assertEquals(2, html.split("Cross-month training", -1).length - 1);
        }
    }

    @Test
    void leapFebruaryIncludes29DaysAndFullWeeks() throws Exception {
        MvcResult result = mvc.perform(get("/training-calendar")
                .param("year", "2024").param("month", "2")
                .sessionAttr("loggedInUser", staff))
                .andExpect(status().isOk()).andReturn();
        List<?> weeks = (List<?>) result.getModelAndView().getModel().get("calendarWeeks");
        int days = 0;
        for (Object row : weeks) {
            List<?> week = (List<?>) row;
            assertEquals(7, week.size());
            for (Object day : week) {
                if (day != null) days++;
            }
        }
        assertEquals(29, days);
        assertEquals(1, ((List<?>) weeks.get(0)).get(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "13", "abc"})
    void invalidMonthIsRejected(String month) throws Exception {
        mvc.perform(get("/training-calendar").param("month", month)
                .sessionAttr("loggedInUser", staff))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(applications);
    }

    private Employee employee(Long id, String name, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        employee.setUsername("user" + id);
        employee.setRole(role);
        employee.setDesignation(EmployeeDesignation.PROFESSIONAL);
        when(employees.findById(id)).thenReturn(Optional.of(employee));
        return employee;
    }

    private String html(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

}
