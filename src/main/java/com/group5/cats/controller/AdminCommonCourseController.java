package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.dto.CommonCourseForm;
import com.group5.cats.model.CommonCourse;
import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.service.CommonCourseService;
import com.group5.cats.service.CategoryService;
import com.group5.cats.service.TrainingProviderService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminCommonCourseController {

    private final CommonCourseService commonCourseService;
    private final CategoryService categoryService;
    private final TrainingProviderService trainingProviderService;

    public AdminCommonCourseController(
            CommonCourseService commonCourseService,
            CategoryService categoryService,
            TrainingProviderService trainingProviderService) {

        this.commonCourseService = commonCourseService;
        this.categoryService = categoryService;
        this.trainingProviderService = trainingProviderService;
    }

    @GetMapping("/admin/common-courses")
    public String showCommonCourses(HttpSession session, Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        model.addAttribute(
                "commonCourses",
                commonCourseService.findAllCommonCourses()
        );

        return "admin/common-courses";
    }

    @GetMapping("/admin/common-courses/new")
    public String showNewCommonCourseForm(
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

        CommonCourseForm commonCourseForm = new CommonCourseForm();


        model.addAttribute("commonCourseForm", commonCourseForm);
        model.addAttribute(
                "categories",
                categoryService.findAllCategories());

        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders());

        return "admin/common-course-form";
    }

    @PostMapping("/admin/common-courses/save")
    public String saveCommonCourse(
            @ModelAttribute("commonCourseForm")
            CommonCourseForm commonCourseForm,
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
                    "Please enter valid course information."
            );
            model.addAttribute(
                    "categories",
                    categoryService.findAllCategories());

            model.addAttribute(
                    "providers",
                    trainingProviderService.findAllProviders());

            return "admin/common-course-form";
        }

        String error = commonCourseService.createCommonCourse(
                commonCourseForm
        );

        if (error != null) {
            model.addAttribute("errorMessage", error);
            model.addAttribute(
                    "categories",
                    categoryService.findAllCategories());

            model.addAttribute(
                    "providers",
                    trainingProviderService.findAllProviders());

            return "admin/common-course-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Commonly attended course added to the catalogue."
        );

        return "redirect:/admin/common-courses";
    }

    @GetMapping("/admin/common-courses/{id}/edit")
    public String showEditCommonCourseForm(
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

        CommonCourse commonCourse =
                commonCourseService.findCommonCourseById(id);

        if (commonCourse == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Commonly attended course not found."
            );

            return "redirect:/admin/common-courses";
        }

        CommonCourseForm commonCourseForm = new CommonCourseForm();

        commonCourseForm.setTitle(commonCourse.getTitle());

        commonCourseForm.setCategoryId(
                commonCourse.getCategory().getId());

        commonCourseForm.setProviderId(
                commonCourse.getProvider().getId());

        commonCourseForm.setFee(commonCourse.getFee());

        commonCourseForm.setIntroduction(
                commonCourse.getIntroduction());

        model.addAttribute("commonCourseId", commonCourse.getId());
        model.addAttribute("commonCourseForm", commonCourseForm);
        model.addAttribute(
                "categories",
                categoryService.findAllCategories());

        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders());

        return "admin/common-course-edit";
    }

    @PostMapping("/admin/common-courses/{id}/update")
    public String updateCommonCourse(
            @PathVariable("id") Long id,
            @ModelAttribute("commonCourseForm")
            CommonCourseForm commonCourseForm,
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

        CommonCourse commonCourse =
                commonCourseService.findCommonCourseById(id);

        if (commonCourse == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Commonly attended course not found."
            );

            return "redirect:/admin/common-courses";
        }

        model.addAttribute("commonCourseId", id);
        model.addAttribute(
                "categories",
                categoryService.findAllCategories());

        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders());

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "errorMessage",
                    "Please enter valid course information."
            );

            return "admin/common-course-edit";
        }

        String error = commonCourseService.updateCommonCourse(
                id,
                commonCourseForm
        );

        if (error != null) {
            model.addAttribute("errorMessage", error);

            return "admin/common-course-edit";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Commonly attended course updated successfully."
        );

        return "redirect:/admin/common-courses";
    }

    @PostMapping("/admin/common-courses/{id}/delete")
    public String deleteCommonCourse(
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

        String error = commonCourseService.deleteCommonCourse(id);

        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Commonly attended course deleted."
            );
        }

        return "redirect:/admin/common-courses";
    }
}
