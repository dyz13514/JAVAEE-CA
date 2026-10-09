package com.group5.cats.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.TrainingCalendarService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class TrainingCalendarController {

    private final TrainingCalendarService trainingCalendarService;
    private final EmployeeRepository employeeRepository;

    public TrainingCalendarController(
            TrainingCalendarService trainingCalendarService,
            EmployeeRepository employeeRepository) {

        this.trainingCalendarService = trainingCalendarService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/training-calendar")
    public String showTrainingCalendar(
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session,
            HttpServletResponse response,
            Model model) {

        Employee loggedInUser = getCurrentUser(session);

        if (loggedInUser == null) {
            return "redirect:/login";
        }

        addNavigationDetails(loggedInUser, model);

        // Opening the calendar without an employee ID always shows your own.
        Long selectedEmployeeId = employeeId;
        if (selectedEmployeeId == null) {
            selectedEmployeeId = loggedInUser.getId();
        }

        model.addAttribute(
                "isOwnCalendar",
                loggedInUser.getId().equals(selectedEmployeeId)
        );

        LocalDate today = LocalDate.now();

        Integer selectedYear;
        Integer selectedMonth;

        if (year == null) {
            selectedYear = today.getYear();
        } else {
            selectedYear = year;
        }

        if (month == null) {
            selectedMonth = today.getMonthValue();
        } else {
            selectedMonth = month;
        }

        List<CourseApplication> applications = new ArrayList<>();

        List<List<Integer>> calendarWeeks = new ArrayList<>();

        Map<Integer, List<CourseApplication>> applicationsByDay =
                new HashMap<>();

        try {
            Employee calendarEmployee = trainingCalendarService.findCalendarEmployee(
                    loggedInUser,
                    selectedEmployeeId
            );

            model.addAttribute("calendarEmployee", calendarEmployee);

            applications =
                    trainingCalendarService.findApprovedApplicationsByMonth(
                            loggedInUser,
                            selectedEmployeeId,
                            selectedYear,
                            selectedMonth
                    );

            LocalDate monthStart = LocalDate.of(
                    selectedYear,
                    selectedMonth,
                    1
            );

            calendarWeeks = buildCalendarWeeks(monthStart);

            applicationsByDay = buildApplicationsByDay(
                    monthStart,
                    applications
            );

        } catch (SecurityException exception) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("errorMessage", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("applications", applications);
        model.addAttribute("calendarWeeks", calendarWeeks);
        model.addAttribute("applicationsByDay", applicationsByDay);

        return "training-calendar";
    }

    @GetMapping("/training-calendar/employees")
    public String showCalendarEmployees(
            HttpSession session,
            HttpServletResponse response,
            Model model) {

        Employee loggedInUser = getCurrentUser(session);

        if (loggedInUser == null) {
            return "redirect:/login";
        }

        addNavigationDetails(loggedInUser, model);

        List<Employee> employees = new ArrayList<>();

        try {
            employees = trainingCalendarService.findEmployeesForCalendar(loggedInUser);
        } catch (SecurityException exception) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("errorMessage", exception.getMessage());
        }

        model.addAttribute("employees", employees);
        return "training-calendar-employees";
    }

    private Employee getCurrentUser(HttpSession session) {

        Employee sessionEmployee =
                (Employee) session.getAttribute("loggedInUser");

        if (sessionEmployee == null || sessionEmployee.getId() == null) {
            return null;
        }

        // Read the current role from the database in case an admin changed it.
        return employeeRepository.findById(sessionEmployee.getId()).orElse(null);
    }

    private void addNavigationDetails(Employee currentUser, Model model) {

        boolean canViewOthers = currentUser.getRole() == EmployeeRole.MANAGER
                || currentUser.getRole() == EmployeeRole.ADMIN;

        String homeUrl;
        if (currentUser.getRole() == EmployeeRole.ADMIN) {
            homeUrl = "/admin/home";
        } else {
            homeUrl = "/employee/home";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("canViewOthers", canViewOthers);
        model.addAttribute("homeUrl", homeUrl);
    }

    private List<List<Integer>> buildCalendarWeeks(
            LocalDate monthStart) {

        List<List<Integer>> weeks = new ArrayList<>();
        List<Integer> week = new ArrayList<>();

        int firstWeekday = monthStart.getDayOfWeek().getValue();
        int daysInMonth = monthStart.lengthOfMonth();

        // Monday is 1 and Sunday is 7.
        // Fill the empty cells before the first day of the month.
        for (int weekday = 1; weekday < firstWeekday; weekday++) {
            week.add(null);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            week.add(day);

            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }

        // Fill the empty cells after the last day of the month.
        if (!week.isEmpty()) {
            while (week.size() < 7) {
                week.add(null);
            }

            weeks.add(week);
        }

        return weeks;
    }

    private Map<Integer, List<CourseApplication>>
            buildApplicationsByDay(
                    LocalDate monthStart,
                    List<CourseApplication> applications) {

        Map<Integer, List<CourseApplication>> applicationsByDay =
                new HashMap<>();

        int daysInMonth = monthStart.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {

            LocalDate date = monthStart.withDayOfMonth(day);

            List<CourseApplication> dailyApplications =
                    new ArrayList<>();

            for (CourseApplication application : applications) {

                LocalDate fromDate = application.getFromDate();
                LocalDate toDate = application.getToDate();

                if (!date.isBefore(fromDate)
                        && !date.isAfter(toDate)) {

                    dailyApplications.add(application);
                }
            }

            applicationsByDay.put(day, dailyApplications);
        }

        return applicationsByDay;
    }
}
