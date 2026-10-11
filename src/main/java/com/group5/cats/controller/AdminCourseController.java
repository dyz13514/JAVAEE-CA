package com.group5.cats.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CategoryService;
import com.group5.cats.service.CourseService;
import com.group5.cats.service.TrainingProviderService;
import jakarta.servlet.http.HttpSession;

@Controller
public class AdminCourseController {
    private final CourseService courseService;
    private final CategoryService categoryService;
    private final TrainingProviderService providerService;

    public AdminCourseController(CourseService courseService,
            CategoryService categoryService, TrainingProviderService providerService) {
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.providerService = providerService;
    }

    @GetMapping("/admin/courses/new")
    public String showNewCourseForm(HttpSession session, Model model) {
        if (!isLoggedInAdmin(session)) {
            return "redirect:/admin/login";
        }
        addFormData(model, "", null, null, null, "");
        return "admin/course-form";
    }

    @PostMapping("/admin/courses/save")
    public String saveCourse(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "providerId", required = false) Long providerId,
            @RequestParam(value = "fee", required = false) Double fee,
            @RequestParam(value = "introduction", required = false) String introduction,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isLoggedInAdmin(session)) {
            return "redirect:/admin/login";
        }
        String error = courseService.createCourse(title, categoryId, providerId, fee, introduction);
        if (error != null) {
            model.addAttribute("errorMessage", error);
            addFormData(model, title, categoryId, providerId, fee, introduction);
            return "admin/course-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Course created successfully.");
        return "redirect:/admin/common-courses";
    }

    @PostMapping("/admin/courses/{id}/delete")
    public String deleteCourse(@PathVariable("id") Long id,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isLoggedInAdmin(session)) {
            return "redirect:/admin/login";
        }
        String error = courseService.deleteCourse(id);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted successfully.");
        }
        return "redirect:/admin/common-courses";
    }

    private void addFormData(Model model, String title, Long categoryId,
            Long providerId, Double fee, String introduction) {
        model.addAttribute("title", title);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("providerId", providerId);
        model.addAttribute("fee", fee);
        model.addAttribute("introduction", introduction);
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("providers", providerService.findAllProviders());
    }

    private boolean isLoggedInAdmin(HttpSession session) {
        Employee employee = (Employee) session.getAttribute("loggedInUser");
        if (employee == null) {
            return false;
        }
        if (employee.getRole() != EmployeeRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required.");
        }
        return true;
    }
}
