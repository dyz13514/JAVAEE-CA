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

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.AttendanceReportRow;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.BudgetReportRow;
import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.dto.CourseCategory;
import com.group5.cats.dto.CourseFeeDetail;
import com.group5.cats.dto.EmployeeForm;
import com.group5.cats.model.*;

/** Exercises real Thymeleaf fragments and bindings, including populated and empty states. */
class PageRenderingTests {
    @ParameterizedTest
    @ValueSource(strings = {"login", "admin/registrations", "employee-home",
            "apply-course", "my-history", "application-detail", "manager-approvals", "entitlement",
            "manager-reports",
            "admin/home", "admin/notifications", "admin/employees", "admin/employee-form", "admin/employee-edit",
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
                    ? EmployeeRole.ADMIN : template.equals("manager-approvals") || template.equals("manager-reports")
                    ? EmployeeRole.MANAGER : EmployeeRole.REGULAR_STAFF);
            session.setAttribute("loggedInUser", user);
            var response = mvc.perform(get("/preview").param("template", template).param("state", state)
                    .session(session).locale(Locale.ENGLISH)).andReturn().getResponse();
            assertEquals(200, response.getStatus(), template);
            String html = response.getContentAsString(StandardCharsets.UTF_8);
            assertTrue(html.contains("/css/cats.css"), template);
            assertFalse(html.contains("REST asynchronous"), template);
            if (template.equals("admin/notifications")) {
                assertTrue(html.contains("Email notifications") || html.contains("Email Notifications"));
                assertFalse(html.contains("<script>alert(1)</script>"));
                if (!state.equals("empty")) assertTrue(html.contains("&lt;script&gt;"));
            }
            if (template.equals("application-detail")) {
                assertEquals(state.equals("approved"), html.contains("/complete"));
                assertEquals(!state.equals("approved"), html.contains("/withdraw"));
            }
            if (template.equals("apply-course")) {
                assertTrue(html.contains("name=\"courseTitle\""));
                assertTrue(html.contains("action=\"/employee/apply\""));
                assertTrue(html.contains("id=\"summaryTitle\""));
            }
            if (template.equals("manager-reports")) {
                assertTrue(html.contains("Team training reports"), template);
                // The budget tab is rendered for the approved state, the attendance tab otherwise.
                if (state.equals("approved")) {
                    assertTrue(html.contains("Course fee claims and budget utilisation"), template);
                    assertTrue(html.contains("/manager/reports/budget.csv"), template);
                    assertTrue(html.contains("Cloud Architecture Essentials"), template);
                    assertTrue(html.contains("No entitlement record for"), template);
                } else {
                    assertTrue(html.contains("/manager/reports/attendance.csv"), template);
                    assertFalse(html.contains("Course fee claims and budget utilisation"), template);
                }
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
            RegistrationRequest registration = new RegistrationRequest();
            NotificationOutbox notification = new NotificationOutbox();
            notification.setId(10L);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notification.setSubject("Course <script>alert(1)</script>");
            notification.setBody("Manager reason: <script>alert(1)</script>");
            notification.setRecipientEmailSnapshot("manager@example.test");
            notification.setNotificationType(NotificationType.APPLICATION_SUBMITTED);
            notification.setDeliveryStatus(state.equals("approved") ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
            notification.setLastError(state.equals("approved") ? null : "SMTP is not configured.");
            model.addAttribute("notifications", new org.springframework.data.domain.PageImpl<>(
                    state.equals("empty") ? List.of() : List.of(notification)));
            model.addAttribute("statuses", DeliveryStatus.values());
            model.addAttribute("selectedStatus", "");
            registration.setId(7L);
            registration.setName("New Staff");
            registration.setUsername("new.staff");
            registration.setSubmittedAt(java.time.LocalDateTime.now());
            model.addAttribute("registrations", state.equals("empty") ? List.of() : List.of(registration));
            model.addAttribute("registrationHistory", List.of());
            model.addAttribute("pendingRegistrationCount", state.equals("empty") ? 0 : 1);
            if (template.equals("login") && state.equals("empty")) model.addAttribute("accountMode", "register");
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
            model.addAttribute("activeReport", state.equals("approved") ? "budget" : "attendance");
            model.addAttribute("employees", List.of(user));
            model.addAttribute("categories", CourseCategory.filterValues());
            model.addAttribute("selectedEmployeeId", null);
            model.addAttribute("selectedCategory", CourseCategory.ALL);
            model.addAttribute("startDate", LocalDate.of(2026, 1, 1));
            model.addAttribute("endDate", LocalDate.of(2026, 12, 31));
            model.addAttribute("selectedYear", 2026);
            model.addAttribute("attendanceReport", attendanceReport(user, state.equals("empty")));
            model.addAttribute("budgetReport", budgetReport(user, allowance, state.equals("empty")));
            return template;
        }

        private AttendanceReport attendanceReport(Employee user, boolean empty) {
            AttendanceReport report = new AttendanceReport();
            report.setFromDate(LocalDate.of(2026, 1, 1));
            report.setToDate(LocalDate.of(2026, 12, 31));
            report.setCategory(CourseCategory.ALL);
            if (empty) {
                report.setRows(List.of());
                return report;
            }
            AttendanceReportRow row = new AttendanceReportRow();
            row.setEmployeeName(user.getName());
            row.setCourseTitle("Cloud Architecture Essentials");
            row.setCategory(CourseCategory.EXTERNAL);
            row.setFromDate(LocalDate.of(2026, 10, 19));
            row.setToDate(LocalDate.of(2026, 10, 21));
            row.setTrainingDays(3);
            row.setStatus("APPROVED");
            row.setFee(650);
            report.setRows(List.of(row));
            return report;
        }

        private BudgetReport budgetReport(Employee user, EntitlementSummary allowance, boolean empty) {
            BudgetReport report = new BudgetReport();
            report.setYear(2026);
            if (empty) {
                report.setRows(List.of());
                return report;
            }
            BudgetReportRow configured = new BudgetReportRow();
            configured.setEmployeeName(user.getName());
            configured.setEntitlementYear(2026);
            configured.setHasEntitlement(true);
            configured.setTrainingBudget(allowance.getTrainingBudget());
            configured.setClaimedFees(allowance.getOccupiedBudget());
            configured.setRemainingBudget(allowance.getRemainingBudget());
            configured.setUtilisationPercent(38.33);
            CourseFeeDetail detail = new CourseFeeDetail();
            detail.setCourseTitle("Cloud Architecture Essentials");
            detail.setCategory("EXTERNAL");
            detail.setFromDate(LocalDate.of(2026, 10, 19));
            detail.setToDate(LocalDate.of(2026, 10, 21));
            detail.setStatus("APPROVED");
            detail.setFee(650);
            detail.setCountsTowardsBudget(true);
            configured.setDetails(List.of(detail));

            BudgetReportRow notConfigured = new BudgetReportRow();
            notConfigured.setEmployeeName("New Starter");
            notConfigured.setEntitlementYear(2026);
            notConfigured.setHasEntitlement(false);

            report.setRows(List.of(configured, notConfigured));
            return report;
        }
    }
}
