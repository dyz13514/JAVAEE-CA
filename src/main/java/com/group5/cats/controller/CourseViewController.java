package com.group5.cats.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.group5.cats.model.CourseApplication;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.repository.CourseApplicationRepository;
import com.group5.cats.repository.EmployeeRepository;
import com.group5.cats.service.TrainingCalendarService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class CourseViewController {
    private final EmployeeRepository employeeRepository;
    private final CourseApplicationRepository applicationRepository;
    private final TrainingCalendarService calendarService;

    public CourseViewController(EmployeeRepository employeeRepository,
            CourseApplicationRepository applicationRepository, TrainingCalendarService calendarService) {
        this.employeeRepository = employeeRepository;
        this.applicationRepository = applicationRepository;
        this.calendarService = calendarService;
    }

    @GetMapping("/course-view")
    public String showCourses(@RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "false") boolean others,
            HttpSession session, HttpServletResponse response, Model model) {
        Employee sessionUser = (Employee) session.getAttribute("loggedInUser");
        if (sessionUser == null) return "redirect:/employee/login";
        Employee user = employeeRepository.findById(sessionUser.getId()).orElse(null);
        if (user == null) return "redirect:/employee/login";
        model.addAttribute("canViewOthers", user.getRole() == EmployeeRole.MANAGER
                || user.getRole() == EmployeeRole.ADMIN);
        model.addAttribute("others", others || (employeeId != null && !employeeId.equals(user.getId())));
        model.addAttribute("employees", new ArrayList<Employee>());
        model.addAttribute("applications", new ArrayList<CourseApplication>());
        try {
            if (others && employeeId == null) {
                model.addAttribute("employees", calendarService.findEmployeesForCalendar(user));
            } else {
                Employee selected = calendarService.findCalendarEmployee(user,
                        employeeId == null ? user.getId() : employeeId);
                List<CourseApplication> applications = applicationRepository.findByEmployee(selected);
                model.addAttribute("selectedEmployee", selected);
                model.addAttribute("ownCourses", selected.getId().equals(user.getId()));
                model.addAttribute("applications", applications);
            }
        } catch (SecurityException | IllegalArgumentException exception) {
            response.setStatus(403);
            model.addAttribute("errorMessage", exception.getMessage());
        }
        return "course-view";
    }
}
