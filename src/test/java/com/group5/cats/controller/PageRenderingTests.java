package com.group5.cats.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.dto.EmployeeForm;
import com.group5.cats.model.*;

/** Exercises real Thymeleaf fragments and bindings, including populated and empty states. */
class PageRenderingTests {
    @ParameterizedTest
    @ValueSource(strings = {"login", "employee-home",
            "apply-course", "my-history", "application-detail", "manager-approvals", "entitlement",
            "admin/home", "admin/employees", "admin/employee-form", "admin/employee-edit",
            "admin/providers", "admin/provider-form", "admin/provider-edit", "admin/common-courses",
            "admin/common-course-form", "admin/common-course-edit", "admin/public-holidays",
            "admin/public-holiday-form", "admin/public-holiday-edit", "error/404"})
    void templatesRenderWithRealBindings(String template) throws Exception {
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver);
        var views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        var mvc = MockMvcBuilders.standaloneSetup(new PreviewController()).setViewResolvers(views).build();
        for (String state : List.of("populated", "empty", "approved")) {
            var session = new MockHttpSession();
            Employee user = employee();
            user.setRole(template.startsWith("admin/") || template.equals("entitlement")
                    ? EmployeeRole.ADMIN : template.equals("manager-approvals")
                    ? EmployeeRole.MANAGER : EmployeeRole.REGULAR_STAFF);
            session.setAttribute("loggedInUser", user);
            var response = mvc.perform(get("/preview").param("template", template).param("state", state)
                    .session(session).locale(Locale.ENGLISH)).andReturn().getResponse();
            assertEquals(200, response.getStatus(), template);
            String html = response.getContentAsString(StandardCharsets.UTF_8);
            assertTrue(html.contains("/css/cats.css"), template);
            assertFalse(html.contains("REST asynchronous"), template);
            if (template.equals("application-detail")) {
                assertEquals(state.equals("approved"), html.contains("/complete"));
                assertEquals(!state.equals("approved"), html.contains("/withdraw"));
            }
            if (template.equals("apply-course")) {
                assertTrue(html.contains("name=\"courseTitle\""));
                assertTrue(html.contains("action=\"/employee/apply\""));
                assertTrue(html.contains("id=\"summaryTitle\""));
            }
            String previewDir = System.getProperty("cats.previewDir");
            if (previewDir != null) {
                Path file = Path.of(previewDir, template.replace('/', '-') + "-" + state + ".html");
                Files.createDirectories(file.getParent());
                Files.writeString(file, html, StandardCharsets.UTF_8);
            }
        }
    }

    private static Employee employee() {
        Employee user = new Employee();
        user.setId(1L);
        user.setName("Jamie Tan");
        user.setUsername("jamie");
        user.setDesignation(EmployeeDesignation.PROFESSIONAL);
        return user;
    }

    @Controller
    static class PreviewController {
        @GetMapping("/preview")
        String preview(@RequestParam String template, @RequestParam String state, Model model) {
            Employee user = employee();
            user.setRole(EmployeeRole.REGULAR_STAFF);
            CourseApplication course = new CourseApplication();
            course.setId(1L);
            course.setEmployee(user);
            course.setCourseTitle("Cloud Architecture Essentials");
            course.setCategory("EXTERNAL");
            course.setProvider("NUS-ISS");
            course.setFromDate(LocalDate.of(2026, 10, 19));
            course.setToDate(LocalDate.of(2026, 10, 21));
            course.setFee(650);
            course.setJustification("Develop cloud architecture skills for our next project.");
            course.setHalfDay(false);
            course.setStatus(state.equals("approved") ? "APPROVED" : "APPLIED");
            EntitlementSummary allowance = new EntitlementSummary();
            allowance.setEmployeeId(1L);
            allowance.setEmployeeName("Jamie Tan");
            allowance.setEntitlementYear(2026);
            allowance.setTrainingDaysLimit(12);
            allowance.setOccupiedDays(3.5);
            allowance.setRemainingDays(8.5);
            allowance.setTrainingBudget(3000);
            allowance.setOccupiedBudget(1150);
            allowance.setRemainingBudget(1850);
            model.addAllAttributes(Map.of("courseApplication", course, "formAction", "/employee/apply",
                    "applications", state.equals("empty") ? List.of() : List.of(course),
                    "recentApplications", state.equals("empty") ? List.of() : List.of(course),
                    "currentUser", user, "entitlementYear", 2026));
            if (template.equals("login") && state.equals("approved")) {
                model.addAttribute("loginRole", "admin");
            }
            if (!state.equals("empty")) {
                model.addAttribute("allowance", allowance);
                model.addAttribute("entitlement", allowance);
            }
            model.addAllAttributes(Map.of("occupiedDays", 3.5, "remainingDays", 8.5,
                    "occupiedBudget", 1150, "remainingBudget", 1850, "selectedId", 1L,
                    "selectedYear", 2026, "canEdit", true, "toBeViewedEmployees", List.of(user)));
            model.addAllAttributes(Map.of("employeeForm", new EmployeeForm(), "employeeId", 1L,
                    "roles", EmployeeRole.values(), "designations", EmployeeDesignation.values(),
                    "managers", List.of(user), "employees", List.of(user),
                    "commonCourseForm", new CommonCourseForm(), "commonCourseId", 1L));
            model.addAllAttributes(Map.of("providers", List.of(), "providerId", 1L,
                    "commonCourses", List.of(), "holidays", List.of(), "holidayId", 1L,
                    "holidayDate", LocalDate.of(2026, 12, 25)));
            return template;
        }
    }
}
