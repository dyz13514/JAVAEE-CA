package com.group5.cats.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group5.cats.dto.AttendanceReport;
import com.group5.cats.dto.BudgetReport;
import com.group5.cats.dto.CourseCategory;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.ManagerReportCsv;
import com.group5.cats.service.ManagerReportService;

import jakarta.servlet.http.HttpSession;

/**
 * Manager reporting on the manager's own direct reports, with a CSV export per
 * report. Access follows the existing session-based manager pages; the export
 * endpoints answer with status codes instead of redirects.
 */
@Controller
public class ManagerReportController {

    private static final String ATTENDANCE = "attendance";
    private static final String BUDGET = "budget";

    private final ManagerReportService managerReportService;

    public ManagerReportController(ManagerReportService managerReportService) {
        this.managerReportService = managerReportService;
    }

    @GetMapping("/manager/reports")
    public String showManagerReports(
            @RequestParam(value = "report", required = false) String report,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "year", required = false) Integer year,
            HttpSession session,
            Model model) {

        Employee manager = (Employee) session.getAttribute("loggedInUser");
        if (manager == null) {
            return "redirect:/employee/login";
        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return "redirect:/employee/home";
        }

        String activeReport = BUDGET.equals(report) ? BUDGET : ATTENDANCE;
        int currentYear = LocalDate.now().getYear();
        LocalDate defaultFrom = LocalDate.of(currentYear, 1, 1);
        LocalDate defaultTo = LocalDate.of(currentYear, 12, 31);

        LocalDate reportFrom = defaultFrom;
        LocalDate reportTo = defaultTo;
        Integer selectedYear = year == null ? currentYear : year;
        String selectedCategory = category == null || category.isBlank() ? CourseCategory.ALL : category;
        String errorMessage = null;
        Long selectedEmployeeId = employeeId;

        try {
            reportFrom = parseDate(startDate, defaultFrom);
            reportTo = parseDate(endDate, defaultTo);
        } catch (IllegalArgumentException exception) {
            errorMessage = exception.getMessage();
        }

        AttendanceReport attendanceReport = null;
        BudgetReport budgetReport = null;

        if (errorMessage == null) {
            try {
                if (BUDGET.equals(activeReport)) {
                    budgetReport = managerReportService.findBudgetReport(manager, employeeId, selectedYear);
                } else {
                    attendanceReport = managerReportService.findAttendanceReport(
                            manager, employeeId, reportFrom, reportTo, selectedCategory);
                }
            } catch (SecurityException exception) {
                // Hide the unauthorised selection instead of echoing it back.
                errorMessage = exception.getMessage();
                selectedEmployeeId = null;
            } catch (IllegalArgumentException exception) {
                errorMessage = exception.getMessage();
            }
        }

        model.addAttribute("activeReport", activeReport);
        model.addAttribute("employees", managerReportService.findReportableEmployees(manager));
        model.addAttribute("categories", CourseCategory.filterValues());
        model.addAttribute("selectedEmployeeId", selectedEmployeeId);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("startDate", reportFrom);
        model.addAttribute("endDate", reportTo);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("attendanceReport", attendanceReport);
        model.addAttribute("budgetReport", budgetReport);
        model.addAttribute("errorMessage", errorMessage);

        return "manager-reports";
    }

    @GetMapping("/manager/reports/attendance.csv")
    public ResponseEntity<byte[]> exportAttendanceReport(
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "category", required = false) String category,
            HttpSession session) {

        Employee manager = (Employee) session.getAttribute("loggedInUser");
        ResponseEntity<byte[]> denied = deny(manager);
        if (denied != null) {
            return denied;
        }

        LocalDate reportFrom;
        LocalDate reportTo;
        try {
            int currentYear = LocalDate.now().getYear();
            reportFrom = parseDate(startDate, LocalDate.of(currentYear, 1, 1));
            reportTo = parseDate(endDate, LocalDate.of(currentYear, 12, 31));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }

        AttendanceReport report;
        try {
            report = managerReportService.findAttendanceReport(
                    manager, employeeId, reportFrom, reportTo,
                    category == null || category.isBlank() ? CourseCategory.ALL : category);
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }

        String filename = "cats-attendance-report-" + reportFrom + "-to-" + reportTo + ".csv";
        return csvResponse(filename, ManagerReportCsv.attendanceReport(report));
    }

    @GetMapping("/manager/reports/budget.csv")
    public ResponseEntity<byte[]> exportBudgetReport(
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "year", required = false) Integer year,
            HttpSession session) {

        Employee manager = (Employee) session.getAttribute("loggedInUser");
        ResponseEntity<byte[]> denied = deny(manager);
        if (denied != null) {
            return denied;
        }

        int reportingYear = year == null ? LocalDate.now().getYear() : year;

        BudgetReport report;
        try {
            report = managerReportService.findBudgetReport(manager, employeeId, reportingYear);
        } catch (SecurityException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }

        return csvResponse("cats-budget-report-" + reportingYear + ".csv", ManagerReportCsv.budgetReport(report));
    }

    /**
     * Reports cover the direct reports only, so a manager may not read another
     * manager's team. The export endpoints use status codes: 401 when nobody is
     * signed in and 403 for a signed-in non-manager.
     */
    private ResponseEntity<byte[]> deny(Employee manager) {
        if (manager == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (manager.getRole() != EmployeeRole.MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Please enter dates in YYYY-MM-DD format.");
        }
    }
}
