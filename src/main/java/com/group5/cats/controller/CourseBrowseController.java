package com.group5.cats.controller;

import java.time.LocalDate;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import com.group5.cats.service.CourseScheduleService;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.group5.cats.model.Course;
import com.group5.cats.model.Employee;
import com.group5.cats.service.CourseService;
import com.group5.cats.service.TrainingProviderService;
import jakarta.servlet.http.HttpSession;

@Controller
public class CourseBrowseController {
    private final CourseService courseService;
    private final CourseScheduleService scheduleService;

    private final TrainingProviderService providerService;

    public CourseBrowseController(CourseService courseService, TrainingProviderService providerService, CourseScheduleService scheduleService) {
        this.courseService = courseService;
        this.scheduleService = scheduleService;
        this.providerService = providerService;
    }

    @GetMapping("/courses")
    public String browseCourses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) Long providerId,
            @RequestParam(defaultValue = "false") boolean commonOnly,
            HttpSession session, Model model) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) {
            return "redirect:/employee/login";
        }
        List<Long> commonCourseIds = courseService.findCommonCourseIds();
        List<Course> courses = courseService.findCourses(keyword, category, providerId, commonOnly);
        List<Course> featuredCourses = new ArrayList<>();
        for (Course course : courseService.findAllCourses()) {
            if (commonCourseIds.contains(course.getId())) {
                featuredCourses.add(course);
            }
        }
        model.addAttribute("featuredCourses", featuredCourses);
        model.addAttribute("courses", courses);
        model.addAttribute("commonCourseIds", commonCourseIds);
        model.addAttribute("commonOnly", commonOnly);
        model.addAttribute("providers", providerService.findAllProviders());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("selectedProviderId", providerId);
        model.addAttribute("canApply", true);
        return "courses";
    }
    @GetMapping("/courses/{id}")
    public String courseDetails(@PathVariable Long id,
            HttpSession session, Model model, HttpServletResponse response) {
        if (session.getAttribute("loggedInUser") == null) return "redirect:/employee/login";
        Course course = courseService.findCourseById(id);
        if (course == null) {
            response.setStatus(404);
            return "error/404";
        }
        model.addAttribute("course", course);
        model.addAttribute("availableDates", scheduleService.findAvailableDates(course));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("commonCourseIds", courseService.findCommonCourseIds());
        return "course-detail";
    }

}
