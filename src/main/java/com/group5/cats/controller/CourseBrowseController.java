package com.group5.cats.controller;

import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CommonCourseService;
import com.group5.cats.service.CategoryService;
import com.group5.cats.service.TrainingProviderService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CourseBrowseController {
    private final CommonCourseService courseService;
    private final CategoryService categoryService;
    private final TrainingProviderService providerService;

    public CourseBrowseController(CommonCourseService courseService,
            CategoryService categoryService, TrainingProviderService providerService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.providerService = providerService;
    }

    @GetMapping({"/courses", "/employee/courses"})
    public String browseCourses(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long providerId,
            HttpSession session, Model model) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) return "redirect:/employee/login";
        model.addAttribute("courses", courseService.findCommonCourses(keyword, categoryId, providerId));
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("providers", providerService.findAllProviders());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedProviderId", providerId);
        model.addAttribute("canApply", employee.getRole() != EmployeeRole.ADMIN);
        return "courses";
    }

    @GetMapping({"/courses/{id}", "/employee/courses/{id}"})
    public String courseDetails(@PathVariable Long id,
            HttpSession session, Model model, HttpServletResponse response) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) return "redirect:/employee/login";
        CommonCourse course = courseService.findCommonCourseById(id);
        if (course == null) {
            response.setStatus(404);
            return "error/404";
        }
        model.addAttribute("course", course);
        model.addAttribute("canApply", employee.getRole() != EmployeeRole.ADMIN);
        return "course-detail";
    }

    // The separate course/schedule management pages have been replaced by the catalogue.
    @GetMapping("/admin/courses")
    public String oldCoursePage(HttpSession session, HttpServletResponse response) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) return "redirect:/admin/login";
        if (employee.getRole() != EmployeeRole.ADMIN) {
            response.setStatus(403);
            return "error/404";
        }
        return "redirect:/admin/common-courses";
    }
}
