package com.group5.cats.controller;

import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.dto.CourseForm;
import com.group5.cats.model.Course;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CourseService;
import com.group5.cats.service.TrainingProviderService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminCourseController {

    private final CourseService courseService;
    private final TrainingProviderService trainingProviderService;

    public AdminCourseController(
            CourseService courseService,
            TrainingProviderService trainingProviderService) {

        this.courseService = courseService;
        this.trainingProviderService = trainingProviderService;
    }

    @GetMapping("/admin/courses")
    public String showCourses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) Long providerId,
            @RequestParam(defaultValue = "false") boolean commonOnly,
            HttpSession session, Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        return "redirect:/courses";
    }

    @GetMapping("/admin/courses/new")
    public String showNewCourseForm(
            HttpSession session,
            Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        CourseForm courseForm = new CourseForm();
        courseForm.setFee(0.0);

        model.addAttribute("courseForm", courseForm);
        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders()
        );

        return "admin/course-form";
    }

    @PostMapping("/admin/courses/save")
    public String saveCourse(
            @ModelAttribute("courseForm")
            CourseForm courseForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "errorMessage",
                    "Please enter valid values for the training provider and reference fee."
            );
            model.addAttribute(
                    "providers",
                    trainingProviderService.findAllProviders()
            );

            return "admin/course-form";
        }

        String error = courseService.createCourse(
                courseForm
        );

        if (error != null) {
            model.addAttribute("errorMessage", error);
            model.addAttribute(
                    "providers",
                    trainingProviderService.findAllProviders()
            );

            return "admin/course-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Course created successfully."
        );

        return "redirect:/courses";
    }

    @GetMapping("/admin/courses/{id}/edit")
    public String showEditCourseForm(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        Course course =
                courseService.findCourseById(id);

        if (course == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Course not found."
            );

            return "redirect:/courses";
        }

        CourseForm courseForm = new CourseForm();

        courseForm.setTitle(course.getTitle());
        courseForm.setCategory(course.getCategory());
        courseForm.setProviderId(course.getProvider().getId());
        courseForm.setFee(course.getFee());
        courseForm.setIntroduction(course.getIntroduction());
        courseForm.setDurationDays(course.getDurationDays());
        StringBuilder dates = new StringBuilder();
        for (LocalDate date : course.getStartDates()) {
            if (!dates.isEmpty()) dates.append("\n");
            dates.append(date);
        }
        courseForm.setStartDates(dates.toString());

        model.addAttribute("courseId", course.getId());
        model.addAttribute("courseForm", courseForm);
        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders()
        );

        return "admin/course-edit";
    }

    @PostMapping("/admin/courses/{id}/update")
    public String updateCourse(
            @PathVariable("id") Long id,
            @ModelAttribute("courseForm")
            CourseForm courseForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        Course course =
                courseService.findCourseById(id);

        if (course == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Course not found."
            );

            return "redirect:/courses";
        }

        model.addAttribute("courseId", id);
        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders()
        );

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "errorMessage",
                    "Please enter valid values for the training provider and reference fee."
            );

            return "admin/course-edit";
        }

        String error = courseService.updateCourse(
                id,
                courseForm
        );

        if (error != null) {
            model.addAttribute("errorMessage", error);

            return "admin/course-edit";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Course updated successfully."
        );

        return "redirect:/courses";
    }

    @PostMapping("/admin/courses/{id}/delete")
    public String deleteCourse(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        String error = courseService.deleteCourse(id);

        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Course deleted successfully."
            );
        }

        return "redirect:/courses";
    }
}