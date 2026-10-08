package com.group5.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group5.cats.model.Employee;
import com.group5.cats.model.EmployeeRole;
import com.group5.cats.model.TrainingProvider;
import com.group5.cats.service.TrainingProviderService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminTrainingProviderController {

    private final TrainingProviderService trainingProviderService;

    public AdminTrainingProviderController(
            TrainingProviderService trainingProviderService) {

        this.trainingProviderService = trainingProviderService;
    }

    @GetMapping("/admin/providers")
    public String showProviders(HttpSession session, Model model) {

        Employee loggedInUser =
                (Employee) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            return "redirect:/admin/login";
        }

        if (loggedInUser.getRole() != EmployeeRole.ADMIN) {
            return "redirect:/employee/home";
        }

        model.addAttribute(
                "providers",
                trainingProviderService.findAllProviders()
        );

        return "admin/providers";
    }

    @GetMapping("/admin/providers/new")
    public String showNewProviderForm(
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

        model.addAttribute("name", "");

        return "admin/provider-form";
    }

    @PostMapping("/admin/providers/save")
    public String saveProvider(
            @RequestParam("name") String name,
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

        String error = trainingProviderService.createProvider(name);

        if (error != null) {
            model.addAttribute("errorMessage", error);
            model.addAttribute("name", name);

            return "admin/provider-form";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Training provider created successfully."
        );

        return "redirect:/admin/providers";
    }

    @GetMapping("/admin/providers/{id}/edit")
    public String showEditProviderForm(
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

        TrainingProvider provider =
                trainingProviderService.findProviderById(id);

        if (provider == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Training provider not found."
            );

            return "redirect:/admin/providers";
        }

        model.addAttribute("providerId", provider.getId());
        model.addAttribute("name", provider.getName());

        return "admin/provider-edit";
    }

    @PostMapping("/admin/providers/{id}/update")
    public String updateProvider(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
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

        TrainingProvider provider =
                trainingProviderService.findProviderById(id);

        if (provider == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Training provider not found."
            );

            return "redirect:/admin/providers";
        }

        String error = trainingProviderService.updateProvider(id, name);

        if (error != null) {
            model.addAttribute("errorMessage", error);
            model.addAttribute("providerId", id);
            model.addAttribute("name", name);

            return "admin/provider-edit";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Training provider updated successfully."
        );

        return "redirect:/admin/providers";
    }

    @PostMapping("/admin/providers/{id}/delete")
    public String deleteProvider(
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

        String error = trainingProviderService.deleteProvider(id);

        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Training provider deleted successfully."
            );
        }

        return "redirect:/admin/providers";
    }
}